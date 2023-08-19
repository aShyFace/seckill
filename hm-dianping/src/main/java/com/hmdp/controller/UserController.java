package com.hmdp.controller;


import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.hmdp.constant.RedisConstant;
import com.hmdp.constant.RequestConstant;
import com.hmdp.dto.AppHttpCodeEnum;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.log.LogApi;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisCache;
import com.hmdp.utils.UserHolder;
import com.hmdp.utils.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.Min;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author Zhi
 * @since 2021-12-22
 */
@Slf4j
//@LogApi
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;
    @Resource
    private IUserInfoService userInfoService;
    @Resource
    private RedisCache redisCache;





    /**
     * 根据id查询用户
     *
     * @param userId 用户id
     * @return {@link Result}
     */
    @GetMapping("/{id}")
    public Result queryUserById(@Min(1L) @PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }


    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone) {
        return userService.sedCode(phone);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm){
        // 实现登录功能
        return userService.login(loginForm);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request){
        String token = request.getHeader(RequestConstant.TOKEN_HEADER);
        if (!StringUtils.hasText(token)){
            return Result.fail(AppHttpCodeEnum.NEED_LOGIN);
        }

        String user_token_key = String.join("", RedisConstant.LOGIN_USER_TOKEN, token);
        boolean success = redisCache.deleteObject(user_token_key);
        if (success){
            return Result.ok("已退出登陆");
        }
        return Result.fail(AppHttpCodeEnum.SYSTEM_ERROR);
    }

    @GetMapping("/me")
    public Result me(){
        // 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    @PostMapping("/sign")
    public Result sign(){
        boolean success = userInfoService.sign();
        if (success){
            return Result.ok(AppHttpCodeEnum.SUCCESS);
        }
        return Result.fail(500, "系统开小差了， 请重试");
    }

    @PostMapping("/sign/count")
    public Result signCount(){
        int count = userInfoService.signCount();
        return Result.ok(count);
    }


}
