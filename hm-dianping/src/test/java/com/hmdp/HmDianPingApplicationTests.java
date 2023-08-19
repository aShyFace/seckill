package com.hmdp;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.constant.RedisConstant;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class HmDianPingApplicationTests {
  @Resource
  public IShopService shopService;
  @Resource
  private UserMapper userMapper;

  @Resource
  private RedisIdWorker redisIdWorker;
  @Resource
  private RabbitTemplate rabbitTemplate;
  @Resource
  private MQSender mqSender;
  @Resource
  private RedisCache redisCache;

  private ExecutorService es = Executors.newFixedThreadPool(500);

  public HmDianPingApplicationTests() {
  }

  //@Test
  //public void testLoadShop2Redis() {
  //  List<Shop> shopList = shopService.list();
  //  Map<Long, List<Shop>> typeMap = shopList.stream().collect(
  //      Collectors.groupingBy(Shop::getTypeId));
  //
  //  Iterator<Map.Entry<Long, List<Shop>>> iterator = typeMap.entrySet().iterator();
  //  while (iterator.hasNext()){
  //    Map.Entry<Long, List<Shop>> typeEntry = iterator.next();
  //    String typeKey = String.join("",
  //        RedisConstant.SHOP_GEO_TYPE_KEY, typeEntry.getKey().toString());
  //    List<RedisGeoCommands.GeoLocation> locationList = typeEntry.getValue().stream().map(shop -> {
  //        Point pt = new Point(shop.getX(), shop.getY());
  //        // 保存的是shopId，所以这里是Long
  //        return new RedisGeoCommands.GeoLocation<>(shop.getId(), pt);
  //      }
  //    ).collect(Collectors.toList());
  //    redisCache.getRedisTemplate().opsForGeo().add(typeKey, locationList);
  //  }
  //}

  //@Test
  //public void testDelayMag(){
  //  String exchangeName = "amq.direct";
  //  String routingKey = "order";
  //  String msg = "ajs;ldjfhpaosugha";
  //  mqSender.sendMessage(exchangeName, routingKey, msg);
  //}

  //@Test
  //public void testMq(){
  //  String exchangeName = VOUCHER_ORDER_EXCHANGE;
  //  String routingKey = "order1";
  //  String msg = "ajs;ldjfhpaosugha";
  //  mqSender.sendMessage(exchangeName, routingKey, msg);
  //}

  //@Test
  //public void testAddMqMsg(){
  //  String qName = "simple.queue";
  //  String msg = "hello;sldfjag;ldf";
  //  rabbitTemplate.convertAndSend(qName, msg);
  //}

  //@RabbitListener(bindings = @QueueBinding(
  //  value = @Queue("simple.queue"),
  //  exchange = @Exchange("amq.direct"),
  //  key = {"seckill"}
  //))
  //public void listenDirectQueue2(String msg){
  //  System.out.println(String.join("","listenDirectQueue2============", msg, "============"));
  //}
  //
  //@Test
  //public void testIdWork() throws InterruptedException {
  //  CountDownLatch latch = new CountDownLatch(300);
  //  Runnable tesk = () -> {
  //    for (int i = 0; i < 100; i++) {
  //      long id = redisIdWorker.nextId("order");
  //      System.out.println(id);
  //    }
  //    latch.countDown();
  //  };
  //
  //  long st = System.currentTimeMillis();
  //  for (int i = 0; i < 300; i++) {
  //    es.submit(tesk);
  //  }
  //  latch.await();
  //  long et = System.currentTimeMillis();
  //  System.out.println("spend: " + (et - st));
  //}

  @Test
  public void teststart() throws IOException {
    redisCache.addCacheZSet("seckill:voucher:11", 11, 1009L);
    //String tokenFile = "/home/zhi/Documents/jemter/token.txt";
    //BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(tokenFile));
    //for (User user: userMapper.selectList(null) ) {
    //  String token = createTokenFile(user);
    //  bufferedWriter.write(token);
    //  bufferedWriter.newLine();
    //}
    ////刷新流（将缓存区中的数据写入输出流）
    //bufferedWriter.flush();
    ////关闭资源
    //bufferedWriter.close();
  }

  public String createTokenFile(User user) {
    // 7.1 生成随机token
    String token = UUID.randomUUID().toString().replaceAll("-", "");

    // 7.2 user转为hash存储
    UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
    Map<String, Object> userDTOMap = BeanUtil.beanToMap(userDTO);
    String user_token_key = String.join("", RedisConstant.LOGIN_USER_TOKEN, token);
    redisCache.setCacheMap(user_token_key, userDTOMap);

    // 7.3 设置有效期（记得加随机值）
    redisCache.expire(user_token_key, RedisConstant.LOGIN_USER_TOKEN_TTL,
      RedisConstant.LOGIN_USER_TOKEN_TTL_SLAT, TimeUnit.MINUTES);
    return token;
  }


}
