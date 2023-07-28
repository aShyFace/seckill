package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hmdp.constant.RedisConstant;
import com.hmdp.dto.AppHttpCodeEnum;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.log.LogApi;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisCache;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.hmdp.constant.RedisConstant.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@LogApi
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
  @Resource
  private RedisCache redisCache;
  @Resource
  private RedisTemplate redisTemplate;
  @Resource
  private ShopMapper shopMapper;

  @Override
  public Result queryById(Long id) {
    Shop shop = queryWithMutex(id); // 1W,10iter, 9.99k qps
    ////Shop shop = queryWithPassThrough(id);
    //if (Objects.isNull(shop)){
    //  return Result.fail(AppHttpCodeEnum.QUERY_ERROR);
    //}
    //Shop shop = shopMapper.selectById(id); // 1W,10iter,5k qps
    return Result.ok(shop);
  }

  @Override
  @Transactional
  public Result updateShop(Shop shop) {
    String shopKey = String.join("", CACHE_SHOP_KEY, shop.getId().toString());
    shopMapper.updateById(shop);
    redisCache.deleteObject(shopKey);
    return Result.ok();
  }


  /**
   * 缓存穿透（redis中存入空数据）
   * @param id id
   * @return {@link Shop}
   */
  public Shop queryWithPassThrough(Long id){
    String shopKey = String.join("", CACHE_SHOP_KEY, id.toString());
    String shopJson = redisCache.getCacheObject(shopKey);
    // 有数据（第二次来）
    if (StringUtils.hasText(shopJson)) {
      Shop shop = JSON.parseObject(shopJson, Shop.class);
      return shop;
    }
    // 没内容说明已经查了一次数据库，而且没查到，所以返回空（第二次来）
    if (NULL.equals(shopJson)){
      return null;
    }

    // 第二次来的请求不会走下面的代码（除了过期的）
    // 不存在则查询数据库（第一次来或过期）
    Shop shop = shopMapper.selectById(id);
    if (Objects.isNull(shop)) {
      // 数据库里也没有就 存一个空的（避免穿透）
      redisCache.setCacheObject(shopKey, NULL,
        CACHE_SHOP_TTL, CACHE_SHOP_TTL_SLAT, TimeUnit.MINUTES);
      return null;
    }
    // 数据库中有数据则存入redis（第一次来）
    redisCache.setCacheObject(shopKey, JSON.toJSONString(shop),
      CACHE_SHOP_TTL, CACHE_SHOP_TTL_SLAT, TimeUnit.MINUTES);
    return shop;
  }

  public Shop queryWithMutex(Long id){
    String shopKey = String.join("", CACHE_SHOP_KEY, id.toString());
    String shopJson = redisCache.getCacheObject(shopKey);
    Shop shop = null;
    if (StringUtils.hasText(shopJson)) {
      shop = JSON.parseObject(shopJson, Shop.class);
      return shop;
    }
    if (NULL.equals(shopJson)){
      return null;
    }

    // 缓存中无数据则先获取所，才能重建数据库
    String lockKey = String.join("", LOCK_SHOP_KEY, id.toString());
    try{
      boolean tryLock = tryLock(lockKey, LOCK_SHOP_VALUE, LOCK_SHOP_TTL);
      // 有锁执行，无锁等待后再次尝试获取锁
      if (!tryLock) {
        Thread.sleep(50);
        queryWithMutex(id);
      }

      // 获取锁后需要再次确认缓存中是否存在数据，因为我们不能确定拿到锁的那个线程是否对缓存进行了修改
      shopJson = redisCache.getCacheObject(shopKey);
      if (StringUtils.hasText(shopJson)) {
        shop = JSON.parseObject(shopJson, Shop.class);
        return shop;
      }
      if (NULL.equals(shopJson)){
        return null;
      }
      // 缓存中无数据，则查询数据库
      shop = shopMapper.selectById(id);
      if (Objects.isNull(shop)) {
        redisCache.setCacheObject(shopKey, NULL,
          CACHE_SHOP_TTL, CACHE_SHOP_TTL_SLAT, TimeUnit.MINUTES);
        return null;
      }
      redisCache.setCacheObject(shopKey, JSON.toJSONString(shop),
        CACHE_SHOP_TTL, CACHE_SHOP_TTL_SLAT, TimeUnit.MINUTES);
    } catch (InterruptedException e){

    }finally {
      unLock(lockKey);
    }

    return shop;
  }


  public void saveShop2Redis(Long id, Long expireSecond){
    Shop shop = shopMapper.selectById(id);
    LocalDateTime dateTime = LocalDateTime.now().plusSeconds(expireSecond);
    RedisData redisData = new RedisData(shop, dateTime);
    String sk = String.join("", CACHE_SHOP_KEY, id.toString());
    redisCache.setCacheObject(sk, redisData);
  }

  private boolean tryLock(String key, String value, Long ttl){
    Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl, TimeUnit.SECONDS);
    return BooleanUtil.isTrue(result);
  }
  private void unLock(String key){
    Boolean result = redisTemplate.delete(key);
  }


}
