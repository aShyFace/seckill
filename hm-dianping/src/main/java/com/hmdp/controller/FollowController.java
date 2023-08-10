package com.hmdp.controller;


import cn.hutool.core.bean.BeanUtil;
import com.hmdp.constant.SystemConstants;
import com.hmdp.dto.AppHttpCodeEnum;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.log.LogApi;
import com.hmdp.service.FollowService;
import com.hmdp.service.IUserService;
import com.sun.istack.internal.NotNull;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.jnlp.FileSaveService;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@LogApi
@RestController
@RequestMapping("/follow")
public class FollowController {
  @Resource
  private FollowService followService;
  @Resource
  private IUserService userService;


  @PutMapping("/{id}/{isFollow}")
  public Result updateFollow(@Min(1L) @PathVariable("id")Long followUserId, @PathVariable("isFollow")Boolean isFollow) {
    boolean res = followService.updateFollow(followUserId, isFollow);
    if (res){
      return Result.ok();
    }
    return Result.fail(AppHttpCodeEnum.NEED_LOGIN);
  }

  @GetMapping("/or/not/{id}")
  public Result isFollow(@Min(1L) @PathVariable("id")Long followUserId) {
    Boolean res = followService.isFollow(followUserId);
    if (Objects.nonNull(res)){
      return Result.ok(res);
    }
    return Result.fail(AppHttpCodeEnum.NEED_LOGIN);
  }


  @GetMapping("/common/{id}")
  public Result queryCommonFollow(@Min(1L) @PathVariable("id")Long followUserId) {
    List<UserDTO> followList = followService.queryCommonFollow(followUserId);
    if (Objects.nonNull(followList)){
      if (!followList.isEmpty()){
        return Result.ok(followList);
      }
      return Result.ok(null);
    }
    return Result.fail(AppHttpCodeEnum.NEED_LOGIN);

  }





}
