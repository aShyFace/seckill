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
import com.hmdp.log.LogApi;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherService;
import com.hmdp.service.VoucherOrderService;
import com.hmdp.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.hmdp.constant.RedisConstant.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * (TbVoucherOrder)表服务实现类
 *
 * @author makejava
 * @since 2023-08-01 14:03:31
 */
@Slf4j
@LogApi
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
    @Resource
    private RedissonClient redissonClient;

    private VoucherOrderService proxy;
    private BlockingQueue<VoucherOrder> orderTacks = new ArrayBlockingQueue<>(1024 * 1024);
    private static final ExecutorService CERATE_ORDER = Executors.newFixedThreadPool(10);


    @PostConstruct
    private void VoucherOrderHandler(){
        // 在该类创建时调用该方法（不断从阻塞队列中获取订单信息并存入数据库中）
        CERATE_ORDER.submit(() -> {
            while (true){
                try{
                    VoucherOrder voucherOrder = orderTacks.take();
                    Long userId = voucherOrder.getUserId();
                    /*
                        1.多线程的每个线程都拥有自己的锁监视器，导致锁不共享，它们应该使用共同的锁才能避免超买。
                        2.setIfAbsent（也就是setnx）根据key表示不同的锁，value表示拥有锁的线程，这样就避免了锁误删
                    */
                    String lockPrefix = String.join(":", RedisConstant.SECKILL_VOUCHER_ORDER, userId.toString());
                    RLock lock = redissonClient.getLock(lockPrefix);
                    boolean tryLock = lock.tryLock();
                    if (!tryLock){
                        log.error("VoucherOrderHandler：：获取锁失败");
                        return ;
                    }
                    try{
                        // 代理对象的实例中不包含子类特有的方法，所以下面这个方法要在接口中声明
                        proxy.createVoucherOrderByOrder(voucherOrder);
                    }finally {
                        lock.unlock();
                    }
                }catch (Exception e){
                    log.error("订单处理异常", e);
                }
            }
        });
    }

    @Override
    public Result seckillVoucherRedis(Long voucherId) {
        String stockKey = String.join("", SECKILL_STOCK_KEY, voucherId.toString());
        Integer stock = redisCache.getCacheObject(stockKey);
        if (Objects.isNull(stock) || stock < 1) {
            // 没在redis中的商品，或库存不足（这个不是必要条件），就别来抢了
            return Result.fail(AppHttpCodeEnum.QUERY_ERROR);
        }
        // 1.在redis中的商品，才让抢
        Long userId = UserHolder.getUser().getId();
        String orderKey = String.join("", SECKILL_ORDER_KEY, voucherId.toString());
        List<String> keys = Arrays.asList(stockKey, orderKey);
        DefaultRedisScript<Long> script = RedisLua.getSDefaultCRIPT();
        Long result = (Long) redisCache.redisTemplate.execute(script, keys, userId);
        if (Objects.isNull(result)){
            return Result.fail(AppHttpCodeEnum.SYSTEM_ERROR);
        }
        if (result.equals(1L)){
            return Result.fail(AppHttpCodeEnum.SECKILL_FAILD);
        }
        if (result.equals(2L)){
            return Result.fail(AppHttpCodeEnum.ORDER_FAILD);
        }

        // 2.抢到了购买资格，把下单信息保存到阻塞队列中
        long orderId = redisIdWorker.nextId(SECKILL_ORDER_KEY);
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        orderTacks.add(voucherOrder);

        // 获取代理对象后，开启独立线程执行下单任务
        this.proxy = (VoucherOrderService) AopContext.currentProxy();
        return Result.ok(orderId);
    }

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
        RLock lock = redissonClient.getLock(lockPrefix);
        boolean tryLock = lock.tryLock();
        //SimpleRedisLock lock = new SimpleRedisLock(lockPrefix, redisCache.getRedisTemplate());
        //boolean tryLock = lock.tryLock(1200);
        if (!tryLock){
            return Result.fail(400, "未抢到");
        }
        try{
            VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
            // createVoucherOrder默认是 this.createVoucherOrder，代理对象不能用this
            return proxy.createVoucherOrder(voucherId);
        }finally {
            lock.unlock();
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

    @Transactional
    public void createVoucherOrderByOrder(VoucherOrder voucherOrder)   {
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        // 查询放外面就会出现多线程问题，要让他们串行判断
        LambdaQueryWrapper<VoucherOrder> lqw = new LambdaQueryWrapper<>();
        lqw.eq(VoucherOrder::getVoucherId, voucherId).eq(VoucherOrder::getUserId, userId);
        Integer count = voucherOrderMapper.selectCount(lqw);
        if (count > 0) {
            log.error("已抢到，不能再抢");
        }

        // 需要知道update更新是否成功——与是否抢到等价（所以这里用自定义sql查询更新数据的条数）
        int res = seckillVoucherMapper.updateByIdCAS(voucherId);
        if (MethodConstant.FAILD == res){
            log.error("已抢完");
        }
        voucherOrderMapper.insert(voucherOrder);
        log.info("下单成功");
    }
}

