package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.constant.MethodConstant;
import com.hmdp.constant.RedisConstant;
import com.hmdp.dto.AppHttpCodeEnum;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherService;
import com.hmdp.service.VoucherOrderService;
import com.hmdp.utils.RedisCache;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
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
    private VoucherOrderMapper voucherOrderMapper;
    @Resource
    private RedisCache redisCache;
    @Resource
    private RedisIdWorker redisIdWorker;


    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherMapper.selectById(voucherId);
        if (Objects.isNull(seckillVoucher)){
            return Result.fail(AppHttpCodeEnum.QUERY_ERROR);
        }
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail(401, "秒杀还未开始");
        }
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail(401, "秒杀已结束");
        }
        if (seckillVoucher.getStock() < 1){
            return Result.fail(401, "已抢完");
        }

        Long userId = UserHolder.getUser().getId();
        /*
            1.多线程的每个线程都拥有自己的锁监视器，导致锁不共享，它们应该使用共同的锁才能避免超买。
            2.setIfAbsent（也就是setnx）根据key表示不同的锁，value表示拥有锁的线程，这样就避免了锁误删
        */
        String lockPrefix = String.join(":", RedisConstant.SECKILL_VOUCHER_ORDER, userId.toString());
        SimpleRedisLock redisLock = new SimpleRedisLock(lockPrefix, redisCache.getRedisTemplate());
        boolean tryLock = redisLock.tryLock(1200);
        if (!tryLock){
            return Result.fail(400, "未抢到");
        }
        try{
            VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
            // createVoucherOrder默认是 this.createVoucherOrder，代理对象不能用this
            return proxy.createVoucherOrder(voucherId);
        }finally {
            redisLock.unlock();
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId)   {
        Long userId = UserHolder.getUser().getId();
        // 查询放外面就会出现多线程问题，要让他们串行判断
        LambdaQueryWrapper<VoucherOrder> lqw = new LambdaQueryWrapper<>();
        lqw.eq(VoucherOrder::getVoucherId, voucherId).eq(VoucherOrder::getUserId, userId);
        Integer count = voucherOrderMapper.selectCount(lqw);
        if (count > 0) {
            return Result.fail(402, "已抢到，不能再抢");
        }

        // 需要知道update更新是否成功——与是否抢到等价（所以这里用自定义sql查询更新数据的条数）
        int res = seckillVoucherMapper.updateByIdCAS(voucherId);
        if (MethodConstant.FAILD == res){
            return Result.fail(401, "已抢完");
        }
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId(RedisConstant.SECKILL_VOUCHER_ORDER);
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrderMapper.insert(voucherOrder);
        return Result.ok(orderId);
    }
}

