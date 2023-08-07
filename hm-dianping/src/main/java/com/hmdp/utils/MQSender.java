package com.hmdp.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * @author:zhi
 * @Date:2021/1/2
 * @Time:19:47
 **/
@Slf4j
@Component
public class MQSender {

  @Resource
  private RabbitTemplate rabbitTemplate;

  //配置return监听处理，消息无法路由到queue,根据实际业务操作
  final RabbitTemplate.ReturnCallback returnCallback = new RabbitTemplate.ReturnCallback() {
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
      System.out.println("=============returnCallback触发。消息路由到queue失败===========");
      log.info("返回消息回调:{} 应答代码:{} 回复文本:{} 交换器:{} 路由键:{}",
        message, replyCode, replyText, exchange, routingKey);
    }
  };


  /**
   * 如果发送方和接收方在同一项目中，且发送使用的对象为同一个包导入：发送和接受都没有问题。
   * 但是，如果发送方和接收方在两个独立的子模块下，且两个模块类传送对象的代码一模一样：
   *      接收方在接收对象时会产生异常：Could not deserialize object type（即接收到的数据无法反序列化）
   * 综上，还是得转成json
   *
   * @param exchange   交换
   * @param routingKey 路由键
   * @param message    消息
   */
  public void sendMessage(String exchange, String routingKey, String message){
    //设置消息的return监听，当消息无法路由到queue时候，会触发这个监听。
    rabbitTemplate.setReturnCallback(returnCallback);
    //设置消息的confirm监听，监听消息是否到达exchange
    CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString().replaceAll("-", ""));
    correlationData.getFuture().addCallback(
      // 有返回结果时调用ConfirmCallback，这里是匿名函数
      result -> {
        if(result.isAck()){
          // 3.1.ack，消息成功
          log.debug("消息发送成功, ID:{}", correlationData.getId());
        }else{
          // 3.2.nack，消息失败
          log.error("消息未发送到exchange, ID:{}, 原因{}",correlationData.getId(), result.getReason());
        }
      },
      // 无返回结果时调用自定义函数，也是匿名函数
      ex -> log.error("消息发送异常——回调失败， ID:{}, 原因{}",correlationData.getId(),ex.getMessage())
    );
    //correlationDataId相当于消息的唯一表示
    rabbitTemplate.convertAndSend(exchange,routingKey,message,correlationData);

  }


}

