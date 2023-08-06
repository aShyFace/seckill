package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    /**
     * 开始时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    /**
     * 序列号的位数
     */
    private static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix) {
        // 1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2.生成序列号
        // 2.1.获取当前日期，精确到天（redis中，以冒号为分隔的会创建树形结构保存）
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        /*
        * 2.2.自增长（increment方法会自动创建key，相同key调用该方法时，key对应的value会自增1）
        *       key采用 业务名+时间戳，以防单个key超过2^32（redis int的存储上限）
        * */
        long count = stringRedisTemplate.opsForValue().increment(String.join("", "icr:", keyPrefix, date));

        // 3.拼接并返回
        return timestamp << COUNT_BITS | count;
    }
}
