package com.hmdp;

import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import sun.print.CUPSPrinter;

import javax.annotation.Resource;

@SpringBootTest
@RunWith(SpringRunner.class)
public class HmDianPingApplicationTests {
  @Resource
  public ShopServiceImpl shopServiceImpl;

  @Test
  public void testSaveShop(){
    shopServiceImpl.saveShop2Redis(2L, 10L);
  }


}
