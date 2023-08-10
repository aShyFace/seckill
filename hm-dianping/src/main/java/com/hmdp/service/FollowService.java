package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;

import java.util.List;

/**
 * (Follow)表服务接口
 *
 * @author makejava
 * @since 2023-08-09 20:01:05
 */
public interface FollowService extends IService<Follow> {

  boolean updateFollow(Long followUserId, Boolean isFollow);

  boolean isFollow(Long followUserId);

  List<UserDTO> queryCommonFollow(Long followUserId);

}

