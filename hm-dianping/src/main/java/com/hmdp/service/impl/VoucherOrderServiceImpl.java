package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.constant.MQConstant;
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
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.hmdp.constant.RedisConstant.*;
import static com.hmdp.constant.MQConstant.*;

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
//@LogApi
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
    @Resource
    private MQSender mqSender;

    private VoucherOrderService proxy;
    private BlockingQueue<VoucherOrder> orderTacks = new ArrayBlockingQueue<>(1024 * 1024);
    private static final ExecutorService CERATE_ORDER = Executors.newFixedThreadPool(10);

    public static final String DEFAULT_EXCHANGE = VOUCHER_ORDER_EXCHANGE;



    //@Override
    public Result seckillVoucherRedisMq(Long voucherId) {
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
        // 发送消息到交换机
        String voucherOrderJson = JsonUtil.object2Json(voucherOrder);
        mqSender.sendMessage(DEFAULT_EXCHANGE, ROUING_KEY1, voucherOrderJson);
        this.proxy = (VoucherOrderService) AopContext.currentProxy();
        return Result.ok(orderId);
    }

    /**
     * 处理券订单
     * @param msg 消息
     *//*
    * @RabbitListener：方法上的注解，声明这个方法是一个消费者方法，需要指定下面的属性：
        @bindings：指定绑定关系，可以有多个。值是@QueueBinding的数组。@QueueBinding包含下面属性：
            @value：这个消费者关联的队列。值是@Queue，代表一个队列
            @exchange：队列所绑定的交换机，值是@Exchange类型
            @key：队列和交换机的对应关系（绑定的BindingKey）
    * */
    //@RabbitListener(bindings = {@QueueBinding(
    //      value = @Queue(VOUCHER_ORDER1),
    //      exchange = @Exchange(value = DEFAULT_EXCHANGE, type = ExchangeTypes.TOPIC),
    //      key = VOUCHER_ORDER_BINDING_KEY1
    //    ), @QueueBinding(
    //      value = @Queue(VOUCHER_ORDER2),
    //      exchange = @Exchange(value = DEFAULT_EXCHANGE, type = ExchangeTypes.TOPIC),
    //      key = VOUCHER_ORDER_BINDING_KEY2
    //    )
    //})
    @RabbitListener(queues = VOUCHER_ORDER1)
    public void handlerVoucherOrder(String msg){
        System.out.println("=================handlerVoucherOrder ing================");
        System.out.println((1/0));
        try{
            VoucherOrder voucherOrder = JsonUtil.json2Object(msg, VoucherOrder.class);
            Long userId = voucherOrder.getUserId();
            // 基于redission的锁，解决锁共享和锁误删问题
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
            // 到了最大重试次数才会报错，也就是管你试错了几次都只报一次的错
            log.error("订单处理异常", e);
        }
        System.out.println("=================handlerVoucherOrder end================");
    }

    /**
     * 直接听队列
     *
     * @param msg 订单消息
     */// 绑定死信队列
    @RabbitListener(bindings = @QueueBinding(
      value = @Queue(ORDER_DELAY_QUEUE),
      exchange = @Exchange(ORDER_DELAY_EXCHANGE),
      key = ORDER_DELAY_ROUTING_KEY
    ))
    public void listenDirectQueue(String msg){
        log.error(String.join("","出现死信，订单信息为========================"));
        //System.out.println(1/0);
        System.out.println(msg);
        log.error(String.join("","end========================"));

    }

    // TODO 惰信队列
    //@RabbitListener(bindings = @QueueBinding(
    //  value = @Queue("order.delay"),
    //  exchange = @Exchange("amq.direct"),
    //  key = {"order", "voucher"}
    //))
    //public void handlerDlQueue(String msg){
    //    System.out.println(String.join("","handlerDlQueue ing============", msg, "============"));
    //    //System.out.println(1/0);
    //    System.out.println(String.join("","handlerDlQueue end============", msg, "============"));
    //
    //}





    /**
     * 从阻塞队列中获取订单对象后，开启独立线程 在数据库中创建订单记录
     */
    @PostConstruct
    private void VoucherOrderHandler(){
        // 在该类创建时调用该方法（不断从阻塞队列中获取订单信息并存入数据库中）
        CERATE_ORDER.submit(() -> {
            while (true){
                try{
                    VoucherOrder voucherOrder = orderTacks.take();
                    Long userId = voucherOrder.getUserId();
                    // 基于redission的锁，解决锁共享和锁误删问题
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

    /**
     * 基于redis的券秒杀
     *
     * @param voucherId 券id
     * @return {@link Result}
     */
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


    /**
     * 基于数据库的券秒杀
     *
     * @param voucherId 券id
     * @return {@link Result}
     */
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

    /**
     * 根据券id 创建订单记录
     *
     * @param voucherId 券id
     * @return {@link Result}
     */
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

    /**
     * 根据券对象 创建订单记录
     *
     * @param voucherOrder 券订单
     */
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
            return ;
        }

        // 需要知道update更新是否成功——与是否抢到等价（所以这里用自定义sql查询更新数据的条数）
        int res = seckillVoucherMapper.updateByIdCAS(voucherId);
        if (MethodConstant.FAILD == res){
            log.error("已抢完");
            return ;
        }
        voucherOrderMapper.insert(voucherOrder);
        log.info("下单成功");
    }
}

