package com.hmdp;

import com.hmdp.service.IShopService;
import com.hmdp.utils.MQSender;
import com.hmdp.utils.RedisCache;
import com.hmdp.utils.RedisIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class HmDianPingApplicationTests {
  @Resource
  public IShopService shopService;

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
  public void teststart(){
    System.out.println("ok");
  }
}
