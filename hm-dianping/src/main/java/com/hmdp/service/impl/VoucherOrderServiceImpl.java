package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.constant.DataBaseConstant;
import com.hmdp.constant.MethodConstant;
import com.hmdp.constant.RedisConstant;
import com.hmdp.dto.AppHttpCodeEnum;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.VoucherOrderService;
import com.hmdp.utils.BeanCopyUtils;
import com.hmdp.utils.RedisCache;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * (TbVoucherOrder)表服务实现类
 *
 * @author makejava
 * @since 2023-08-01 14:03:31
 */
@Service("tbVoucherOrderService")
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements VoucherOrderService {
    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;
    @Resource
    private VoucherOrderService voucherOrderService;
    @Resource
    private RedisCache redisCache;
    @Resource
    private RedisIdWorker redisIdWorker;


    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherMapper.selectById(voucherId);
        if (Objects.isNull(seckillVoucher)){
            System.out.println(seckillVoucher.getStock() + " fail, QUERY_ERROR");
            return Result.fail(AppHttpCodeEnum.QUERY_ERROR);
        }
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            System.out.println(seckillVoucher.getStock() + " fail, 秒杀还未开始");
            return Result.fail(401, "秒杀还未开始");
        }
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            System.out.println(seckillVoucher.getStock() + " fail, 秒杀已结束");
            return Result.fail(401, "秒杀已结束");
        }

        if (seckillVoucher.getStock() < 1){
            System.out.println(seckillVoucher.getStock() + " fail, 已抢完");
            return Result.fail(401, "已抢完");
        }
        // 需要知道update更新是否成功——与是否抢到等价（所以这里用自定义sql查询更新数据的条数）
        int res = seckillVoucherMapper.updateByIdCAS(voucherId);
        if (MethodConstant.FAILD == res){
            System.out.println(seckillVoucher.getStock() + " fail, sql");
            return Result.fail(401, "已抢完");
        }
        UserDTO userDto = UserHolder.getUser();
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId(RedisConstant.SECKILL_VOUCHER_ORDER);
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userDto.getId());
        voucherOrder.setVoucherId(voucherId);
        voucherOrderService.save(voucherOrder);
        System.out.println(seckillVoucher.getStock() + " success " + seckillVoucherMapper.selectById(voucherId).getStock());
        return Result.ok(orderId);
    }
}

