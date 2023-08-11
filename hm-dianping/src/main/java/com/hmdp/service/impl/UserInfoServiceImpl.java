package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.constant.RedisConstant;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.UserInfo;
import com.hmdp.log.LogApi;
import com.hmdp.mapper.UserInfoMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserInfoService;
import com.hmdp.utils.RedisCache;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-24
 */
@LogApi
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {
  @Resource
  private UserMapper userMapper;
  @Resource
  private RedisCache redisCache;


  @Override
  public boolean sign() {
    UserDTO userDTO = UserHolder.getUser();
    // 按月签到，所以用y和m
    LocalDateTime currentTime = LocalDateTime.now();
    int currentDay = currentTime.getDayOfMonth();
    String suffix = currentTime.format(DateTimeFormatter.ofPattern(":yyyyMM"));
    String userSignKey = String.join("",
        RedisConstant.USER_SIGN_BITMAP, userDTO.getId().toString(), suffix);
    boolean res = redisCache.setBitMap(userSignKey, currentDay - 1, true);
    return res;
  }


  /**
   * 获取 截止今天为止的连续签到天数
   *
   * @return int
   */
  @Override
  public int signCount() {
    UserDTO userDTO = UserHolder.getUser();
    LocalDateTime currentTime = LocalDateTime.now();
    int currentDay = currentTime.getDayOfMonth();
    String suffix = currentTime.format(DateTimeFormatter.ofPattern(":yyyyMM"));
    String userSignKey = String.join("",
        RedisConstant.USER_SIGN_BITMAP, userDTO.getId().toString(), suffix);
    // 获取本月的连续签到次数（不是最大签到次数），包括第一天开始的签到记录，所以不需要偏移
    List<Long> result = redisCache.getBitMapField(userSignKey, true, currentDay-1, 0);
    if (Objects.isNull(result) || result.isEmpty()) {
      return 0;
    }
    // 每个人的签到记录是唯一的，所以list只有一个元素
    Long num = result.get(0);
    if (Objects.isNull(num) || result.equals(0)){
      return 0;
    }

    int maxCount = 0;
    while (true) {
      if ((num & 1) == 0){
        break;
      }else{
        maxCount ++;
      }
      // >>算术右移 补的是符号位，>>>逻辑右移 补0
      num >>>= 1;
    }
    return maxCount;
  }

}
