package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.log.LogApi;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import com.hmdp.service.VoucherOrderService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.Min;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Zhi
 * @since 2021-12-22
 */
//@LogApi
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Resource
    private VoucherOrderService voucherOrderService;

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@Min(1L) @PathVariable("id") Long voucherId) {
        // 500张票，不然体现不出差距
        /*
        * 使用实现的秒杀，各项指标为（出现无响应的情况，大概有3秒的时间）
        *   平均时长：129mm；最小时长：91mm；最大时长：1553mm；吞吐量：537/sec；
        * */
        Result result = voucherOrderService.seckillVoucherRedisMq(voucherId);

        /* 500张票，不然体现不出差距
        * 使用实现的秒杀，各项指标为
        *   平均时长：157mm；最小时长：86mm；最大时长：1711mm；吞吐量：482/sec；
        * */
        //Result result = voucherOrderService.seckillVoucherRedis(voucherId);

        /* 模拟1008个用户发送100次请求的情况
        * 直接访问数据库实现的秒杀，各项指标为（出现无响应的情况，大概有8秒的时间）
        *   平均时长：325mm；最小时长：116mm；最大时长：2282mm；吞吐量：285/sec；
        * */
        //Result result = voucherOrderService.seckillVoucher(voucherId);
        return Result.ok(result);
    }


}
