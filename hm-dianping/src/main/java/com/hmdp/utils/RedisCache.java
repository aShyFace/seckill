package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.hmdp.constant.RedisConstant;
import com.hmdp.entity.Shop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import static com.hmdp.constant.RedisConstant.*;



@SuppressWarnings(value = { "unchecked", "rawtypes" })
@Component
public class RedisCache
{
    @Resource
    public RedisTemplate redisTemplate;
    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public Long getRandomTTL(final Long timeout, final Long slat){
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        Long min = timeout - slat, max = timeout + slat;
        // 区间长度 + 区间最小值
        Long ttl = min + (((long) (rand.nextDouble() * (max - min))));
        return ttl;
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value)
    {
        // 没有key-value则新增，有则覆盖
        redisTemplate.opsForValue().set(key, value);
    }

    public void setObjectWithLogicalExpire(final String key, final Object value, final Long timeout, final TimeUnit timeUnit)
    {
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        LocalDateTime localTime = LocalDateTime.now().plusSeconds(timeUnit.toSeconds(timeout));
        RedisData redisData = new RedisData(value, localTime);
        // 没有key-value则新增，有则覆盖
        redisTemplate.opsForValue().set(key, redisData);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     * @param timeout 时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final Long timeout, final TimeUnit timeUnit)
    {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 设置缓存对象
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  失效时间
     * @param slat     盐值
     * @param timeUnit 时间单位
     */
    public <T> void setCacheObject(final String key, final T value, final Long timeout, final Long slat, final TimeUnit timeUnit)
    {
        Long ttl = getRandomTTL(timeout, slat);
        redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
    }
    public void setCacheObject2Json(final String key, final Object value)
    {
        String jsonString = JsonUtil.object2Json(value);
        redisTemplate.opsForValue().set(key, jsonString);
    }
    public void setCacheObject2Json(final String key, final Object value, final Long timeout, final Long slat, final TimeUnit timeUnit)
    {
        Long ttl = getRandomTTL(timeout, slat);
        String jsonString = JsonUtil.object2Json(value);
        redisTemplate.opsForValue().set(key, jsonString, ttl, timeUnit);
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout)
    {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit)
    {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 到期
     * 设置有效时间
     * @param key     Redis键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @param slat    盐值
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final long slat, final TimeUnit unit)
    {
        Long ttl = getRandomTTL(timeout, slat);
        return redisTemplate.expire(key, ttl, unit);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key)
    {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        T t = operation.get(key);
        return t;
    }

    /**
     * 通过id获取缓存对象
     *
     * @param key        缓存key
     * @param clazz      clazz
     * @param dbFallback 查询数据库的函数
     * @param queryParam 查询参数
     * @param timeout    超时
     * @param slat       随机值范围
     * @param unit       时间单位
     * @return {@link T}
     */
    public <T, E> T queryWithPassThrough(final String key, Class<T> clazz, Function<E, T> dbFallback, E queryParam, final long timeout, final long slat, final TimeUnit unit)
    {
        //查不到返回null
        T data = null;
        String jsonString = getCacheObject(key);
        if (StringUtils.hasText(jsonString)) {
            data = JsonUtil.json2Object(jsonString, clazz);
            return data;
        }
        if (RedisConstant.NULL.equals(jsonString)){
            return data;
        }

        T res = dbFallback.apply(queryParam);
        if (Objects.isNull(res)){
            setCacheObject(key, RedisConstant.NULL, RedisConstant.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        setCacheObject2Json(key, res, timeout, slat, unit);
        return res;
    }

    /**
     * 查询与逻辑到期
     *
     * @param key        key
     * @param lockKey    锁的key
     * @param dbFallback 数据库函数
     * @param queryParam 查询参数
     * @param timeout    超时时间
     * @param unit       时间单位
     * @return {@link T}
     */
    public <T, E> T queryWithLogicalExpire(final String key, String lockKey, Function<E, T> dbFallback, E queryParam, final long timeout, final TimeUnit unit){
        String json = getCacheObject(key);
        // 1.未命中返回空
        if (!StringUtils.hasText(json)) {
          return null;
        }
        // 2.命中则判断是否过期
        RedisData redisData = JsonUtil.json2Object(
            json, new TypeReference<RedisData<T>>(){});
        T t = (T) redisData.getData();
        T tNew = null;
        LocalDateTime expireTime = redisData.getExpireTime();
        // 3.只有过期且获取到了锁的情况下，需要重建缓存
        //String lockKey = String.join("", LOCK_SHOP_KEY, beanId);
        boolean tryLock = tryLock(lockKey, LOCK_SHOP_VALUE, LOCK_SHOP_TTL);
        if (LocalDateTime.now().isAfter(expireTime) && tryLock) {
          // DoubleCheck
          json = getCacheObject(key);
          if (!StringUtils.hasText(json)) {
            return null;
          }
          // 缓存中无数据，则查询数据库
          try {
            tNew = CACHE_REBUILD.submit(() -> {
              T t2 = dbFallback.apply(queryParam);
              setObjectWithLogicalExpire(key, t2, timeout, unit);
              return t2;
            }).get();
          } catch (InterruptedException e) {

          } catch (ExecutionException e) {
            throw new RuntimeException(e);
          } finally {
            unLock(lockKey);
          }

        }
        return Objects.nonNull(tNew)? tNew:t;
    }



    /**
     * 删除单个对象
     *
     * @param key
     */
    public boolean deleteObject(final String key)
    {
        return redisTemplate.delete(key);
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return
     */
    public long deleteObject(final Collection collection)
    {
        return redisTemplate.delete(collection);
    }

    /**
     * 缓存List数据
     *
     * @param key 缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList)
    {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getCacheList(final String key)
    {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 缓存Set
     *
     * @param key 缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet)
    {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext())
        {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheSet(final String key)
    {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap)
    {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }
    public void setCacheObject2Map(final String key, Objects objects)
    {
        Map<String, Object> dataMap = BeanUtil.beanToMap(objects);
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }



    /**
     * 获得缓存的Map
     *
     * @param key
     * @return
     */
    public <T> Map<String, T> getCacheMap(final String key)
    {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 往Hash中存入数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value)
    {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey)
    {
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    /**
     * 修改Hash中的数据（value自增）
     *
     * @param key Redis键
     * @param hKey Hash键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public void incrementCacheMapValue(final String key, final String hKey, Long step)
    {
        redisTemplate.opsForHash().increment(key, hKey, step);
    }

    /**
     * 删除Hash中的数据
     * 
     * @param key
     * @param hkey
     */
    public void delCacheMapValue(final String key, final String hkey)
    {
        HashOperations hashOperations = redisTemplate.opsForHash();
        hashOperations.delete(key, hkey);
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys)
    {
        return redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern)
    {
        return redisTemplate.keys(pattern);
    }


    private boolean tryLock(String key, String value, Long ttl){
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(result);
    }
    private void unLock(String key){
        Boolean result = redisTemplate.delete(key);
    }

    public static final ExecutorService CACHE_REBUILD = Executors.newFixedThreadPool(10);






}