package com.hmdp.service.impl;

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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import static com.hmdp.constant.RedisConstant.CACHE_SHOP_KEY;
import static com.hmdp.constant.RedisConstant.CACHE_SHOP_TTL;
import static com.hmdp.constant.RedisConstant.CACHE_SHOP_TTL_SLAT;

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
  private ShopMapper shopMapper;

  @Override
  public Result queryById(Long id) {
    String shopKey = String.join("", CACHE_SHOP_KEY, id.toString());
    String shopJson = redisCache.getCacheObject(shopKey);
    if (StringUtils.hasText(shopJson)) {
      Shop shop = JSON.parseObject(shopJson, Shop.class);
      return Result.ok(shop);
    }

    // 不存在则查询数据库
    Shop shop = shopMapper.selectById(id);
    if (Objects.isNull(shop)) {
      return Result.fail(AppHttpCodeEnum.QUERY_ERROR);
    }
    // 数据库中有数据则存入redis
    redisCache.setCacheObject(shopKey, JSON.toJSON(shop),
        CACHE_SHOP_TTL, CACHE_SHOP_TTL_SLAT, TimeUnit.MINUTES);
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


}
