package com.hmdp;

import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import sun.print.CUPSPrinter;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@RunWith(SpringRunner.class)
public class HmDianPingApplicationTests {
  @Resource
  public ShopServiceImpl shopServiceImpl;

  @Resource
  private RedisIdWorker redisIdWorker;
  @Resource
  private RabbitTemplate rabbitTemplate;

  private ExecutorService es = Executors.newFixedThreadPool(500);

  //@Test
  //public void testSaveShop() throws InterruptedException {
  //  shopServiceImpl.saveShop2Redis(2L, 10L);
  //}

  @Test
  public void testMq(){
    String qName = "simple.queue";
    String msg = "hello";
    rabbitTemplate.convertAndSend(qName, msg);
  }

  @Test
  public void testIdWork() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(300);
    Runnable tesk = () -> {
      for (int i = 0; i < 100; i++) {
        long id = redisIdWorker.nextId("order");
        System.out.println(id);
      }
      latch.countDown();
    };

    long st = System.currentTimeMillis();
    for (int i = 0; i < 300; i++) {
      es.submit(tesk);
    }
    latch.await();
    long et = System.currentTimeMillis();
    System.out.println("spend: " + (et - st));
  }


}
