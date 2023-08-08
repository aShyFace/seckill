package com.hmdp.constant;

public class MQConstant {
  public static final String VOUCHER_ORDER_EXCHANGE = "amq.topic";

  public static final String VOUCHER_ORDER1 = "seckill.voucher1";
  public static final String VOUCHER_ORDER_BINDING_KEY1 = "order1.#";
  public static final String ROUING_KEY1 = "order1.voucher";
  public static final String VOUCHER_ORDER2 = "seckill.voucher1";
  public static final String VOUCHER_ORDER_BINDING_KEY2 = "order2.#";
  public static final String ROUING_KEY2 = "order2.voucher";


  /** 配置延迟队列 */
  public static final String ORDER_DELAY_QUEUE = "voucher.order.delay";
  public static final String ORDER_DELAY_EXCHANGE = "voucher.order.delay";
  public static final String ORDER_DELAY_ROUTING_KEY = "voucher.order.delay.#";

}
