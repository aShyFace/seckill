package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.hmdp.constant.RedisConstant;
import com.hmdp.constant.RequestConstant;
import com.hmdp.dto.AppHttpCodeEnum;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;


@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Resource
    private RedisCache redisCache;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1. 获取session
        //HttpSession session = request.getSession();
        String token = request.getHeader(RequestConstant.TOKEN_HEADER);
        if (!StringUtils.hasText(token)){
            // 拦截没携带token的请求
            String s = JSON.toJSONString(Result.fail(AppHttpCodeEnum.NEED_LOGIN));
            WebUtils.renderString(response, s);
            return false;
        }
        //2.获取session中的用户
        //Object user = session.getAttribute("user");
        String user_token_key = String.join("", RedisConstant.LOGIN_USER_TOKEN, token);
        Map<String, Object> userDtoMap = redisCache.getCacheMap(user_token_key);
        //3. 判断用户是否存在
        if (userDtoMap.isEmpty()){
            //4. 过期
            String s = JSON.toJSONString(Result.fail(AppHttpCodeEnum.USER_NOT_EXIST));
            WebUtils.renderString(response, s);
            return false;
        }

        //5. 存在 保存用户信息到ThreadLocal
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userDtoMap, new UserDTO(), false);
        UserHolder.saveUser(userDTO);
        // 5.5 刷新token有效期
        redisCache.expire(user_token_key, RedisConstant.LOGIN_USER_TOKEN_TTL,
            RedisConstant.LOGIN_USER_TOKEN_TTL_SLAT, TimeUnit.MINUTES);
        //6. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户
        UserHolder.removeUser();
    }
}
