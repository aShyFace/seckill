package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import com.hmdp.constant.RedisConstant;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;



public class RedisLua {
    // 不同的业务需要不同的锁
    private String fileName;
    private RedisTemplate redisTemplate;
    private List keys;
    private List<Object> args;
    private static final DefaultRedisScript<Long> SCRIPT;
    static {
        SCRIPT = new DefaultRedisScript<>();
        SCRIPT.setLocation(new ClassPathResource(RedisConstant.SECKILL_LUA_SCRIPT_PATH));
        SCRIPT.setResultType(Long.class);
    }

    public static DefaultRedisScript<Long> getSDefaultCRIPT() {
        return SCRIPT;
    }

    public static DefaultRedisScript<Long> getSCRIPT(String fileName) {
        DefaultRedisScript<Long> SCRIPT;
        SCRIPT = new DefaultRedisScript<>();
        SCRIPT.setLocation(new ClassPathResource(fileName));
        SCRIPT.setResultType(Long.class);
        return SCRIPT;
    }
}
