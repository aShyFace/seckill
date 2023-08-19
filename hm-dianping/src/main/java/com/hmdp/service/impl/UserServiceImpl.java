package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.constant.RedisConstant;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.log.LogApi;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisCache;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.constant.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Zhi
 * @since 2021-12-22
 */
@Slf4j
@LogApi
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private RedisCache redisCache;


    @Override
    public Result sedCode(String phone) {
        //1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }

        //3. 符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4. 保存验证码到session
        //session.setAttribute("code",code);
        String codeKey = String.join("", RedisConstant.LOGIN_CODE_KEY, phone);
        redisCache.setCacheObject(codeKey,code,
            RedisConstant.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //5. 发送验证码
        log.debug("发送短信验证码成功，验证码:{}",code);
        //返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        //1. 校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        //2. 校验验证码
        //Object cacheCode = session.getAttribute("code");
        String cacheCode = redisCache.getCacheObject(String.join("", RedisConstant.LOGIN_CODE_KEY, phone));
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)){
            //3. 不一致，报错
            return Result.fail("验证码错误");
        }

        //4.一致，根据手机号查询用户
        User user = query().eq("phone", phone).one();

        //5. 判断用户是否存在
        if (user == null){
            //6. 不存在，创建新用户
            user = createUserWithPhone(phone);
        }

        //7.保存用户信息到session
        //session.setAttribute("user",BeanUtil.copyProperties(user,UserDTO.class));
        // 7.1 生成随机token
        String token = UUID.randomUUID().toString();
        // 7.2 user转为hash存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userDTOMap = BeanUtil.beanToMap(userDTO);
        String user_token_key = String.join("", RedisConstant.LOGIN_USER_TOKEN, token);
        redisCache.setCacheMap(user_token_key, userDTOMap);

        // 7.3 设置有效期（记得加随机值）
        redisCache.expire(user_token_key, RedisConstant.LOGIN_USER_TOKEN_TTL,
            RedisConstant.LOGIN_USER_TOKEN_TTL_SLAT, TimeUnit.MINUTES);
        return Result.ok(token);
    }



    private User createUserWithPhone(String phone) {
        // 1.创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 2.保存用户
        save(user);
        return user;
    }
}
