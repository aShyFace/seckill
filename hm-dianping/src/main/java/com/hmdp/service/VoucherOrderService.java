package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;

/**
 * (TbVoucherOrder)表服务接口
 *
 * @author makejava
 * @since 2023-08-01 14:03:31
 */
public interface VoucherOrderService extends IService<VoucherOrder> {
  Result seckillVoucherRedisMq(Long voucherId);
  Result seckillVoucherRedis(Long voucherId);

  Result seckillVoucher(Long voucherId);

  Result createVoucherOrder(Long voucherId);

  void createVoucherOrderByOrder(VoucherOrder voucherOrder);
}

