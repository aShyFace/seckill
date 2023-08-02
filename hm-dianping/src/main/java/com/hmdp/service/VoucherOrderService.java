package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;

/**
 * (TbVoucherOrder)表服务接口
 *
 * @author makejava
 * @since 2023-08-01 14:03:31
 */
public interface VoucherOrderService extends IService<VoucherOrder> {

  Result seckillVoucher(Long voucherId);
}

