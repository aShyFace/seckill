package com.hmdp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static com.hmdp.constant.MQConstant.*;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@Configuration
public class RabbitMqConfig {
    private static final String ORDER_QUEUE = VOUCHER_ORDER1;
    private static final String ORDER_EXCHANGE = VOUCHER_ORDER_EXCHANGE;
    private static final String ORDER_ROUTING_KEY = VOUCHER_ORDER_BINDING_KEY1;


    @Bean
    public TopicExchange orderDelayExchange(){
        return new TopicExchange(ORDER_EXCHANGE);
    }

    /**
     * 在自己队列上 配置目标死信队列的关系
     *
     * @return {@link Queue}
     */
    @Bean
    public Queue orderDelayQueue(){
        return QueueBuilder.durable(ORDER_QUEUE)
          //消息超出10条未被消费，多出的会进入死信队列
          .withArgument("x-max-length", 10)
          //消息过期时间设置 超出时间未消费成为死信
          .withArgument("x-message-ttl", 25000)
          //死信交换机声明
          .withArgument("x-dead-letter-exchange", ORDER_DELAY_EXCHANGE)
          //死信消息的路由key
          .withArgument("x-dead-letter-routing-key", ORDER_DELAY_ROUTING_KEY)
          .build();
    }

    //死信队列绑定
    @Bean
    public Binding dlxBinding(){
        return BindingBuilder.bind(orderDelayQueue()).to(orderDelayExchange()).with(ORDER_ROUTING_KEY);
    }





    /**
     * 用于测试：使用死信队列实现消息延时发送
     *
     * @return {@link DirectExchange}
     */
    ///**
    // * 如果想实现延时发送消息的功能，那么这里的ORDER_QUEUE必须没有消费者
    // *      不然消息会被消费者消费，导致延时失效
    // * @return {@link Queue}
    // */
    //@Bean
    //public Queue orderDelayQueue(){
    //    return QueueBuilder.durable(ORDER_QUEUE)
    //      .ttl(10000) // 设置队列的超时时间
    //      .deadLetterExchange(ORDER_DELAY_EXCHANGE)
    //      .deadLetterRoutingKey(ORDER_DELAY_ROUTING_KEY)
    //      .build();
    //}




}

