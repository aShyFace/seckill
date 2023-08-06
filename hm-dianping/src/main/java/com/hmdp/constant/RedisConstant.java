package com.hmdp.constant;

public class RedisConstant {
  public static final Long LOGIN_CODE_TTL = 10L;
  public static final String LOGIN_CODE_KEY = "login:code:";

  public static final String LOGIN_USER_TOKEN = "login:token:";
  public static final Long LOGIN_USER_TOKEN_TTL = 7 * 60 * 24L;
  public static final Long LOGIN_USER_TOKEN_TTL_SLAT = 10L;

  public static final String CACHE_SHOP_KEY = "shop:cache:";
  public static final Long CACHE_SHOP_TTL = 60 * 6L;
  public static final Long CACHE_SHOP_TTL_SLAT = 3L;

  public static final String NULL = "";
  public static final Long CACHE_NULL_TTL = 3L;

  public static final String SECKILL_STOCK_KEY = "seckill:stock:";
  /** redis秒杀订单key */
  public static final String SECKILL_ORDER_KEY = "seckill:order:";
  /** 数据库秒杀券key */
  public static final String SECKILL_VOUCHER_ORDER = "voucher:order:";


  public static final String LOCK_SHOP_KEY = "lock:shop:";
  public static final String LOCK_SHOP_VALUE = "1";
  public static final Long LOCK_SHOP_TTL = 10L;


  public static final String SECKILL_LUA_SCRIPT_PATH = "seckill.lua";
}
