package com.hmdp.listener;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TestListener {


  @RabbitListener(bindings = @QueueBinding(
    value = @Queue("simple.queue"),
    exchange = @Exchange("amq.direct"),
    key = {"order", "voucher"}
  ))
  public void listenDirectQueue(String msg){
    System.out.println(String.join("","listenDirectQueue ing============", msg, "============"));
    System.out.println(1/0);
    System.out.println(String.join("","listenDirectQueue end============", msg, "============"));

  }
}
