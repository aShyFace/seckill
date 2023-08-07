package com.hmdp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class RabbitMqConfig {
    //@Bean
    //public Queue spikeQueue(){
    //    return new Queue("spike_good");
    //}
    //
    //@Bean
    //public TopicExchange spikeTopicExchange(){
    //    return new TopicExchange("spikeTopicExchange");
    //}
    //
    //@Bean
    //public Binding spikeBinding(Queue spikeQueue, TopicExchange spikeTopicExchange){
    //    return BindingBuilder.bind(spikeQueue).to(spikeTopicExchange).with("spike.commodity");
    //}

    private static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    private static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    private static final String ORDER_DELAY_ROUTING_KEY = "order.delay.routingKey";

    private static final String ORDER_QUEUE = "order.queue";
    private static final String ORDER_EXCHANGE = "order.exchange";
    private static final String ORDER_ROUTING_KEY = "order.routingKey";



    //订单交换机
    @Bean
    public DirectExchange orderDirectExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    //订单队列
    @Bean
    public Queue orderQueue() {
        return new Queue(ORDER_QUEUE);
    }
//
    //订单绑定
    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue()).to(orderDirectExchange()).with(ORDER_ROUTING_KEY);
    }

    //订单死信交换机
    @Bean
    public DirectExchange orderDelayExchange(){
        return new DirectExchange(ORDER_DELAY_EXCHANGE);
    }

    //死信队列
    @Bean
    public Queue orderDelayQueue(){//延时队列的配置
        Map<String, Object> params = new HashMap<>(2);
        // x-dead-letter-exchange 声明了队列里的死信转发到的DLX名称，使用order队列原来的exchange
        params.put("x-dead-letter-exchange", ORDER_EXCHANGE);
        // x-dead-letter-routing-key 声明了这些死信在转发时携带的 routing-key 名称。
        params.put("x-dead-letter-routing-key", ORDER_ROUTING_KEY);
        return new Queue(ORDER_DELAY_QUEUE, true, false, false, params);
    }

    //死信队列绑定
    @Bean
    public Binding dlxBinding(){
        return BindingBuilder.bind(orderDelayQueue()).to(orderDelayExchange()).with(ORDER_DELAY_ROUTING_KEY);
    }
}

