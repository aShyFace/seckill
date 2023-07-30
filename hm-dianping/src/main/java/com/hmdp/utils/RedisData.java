package com.hmdp.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisData<T> implements Serializable {
    private static final long serialVersionUID = 1L;


    private T data;
    private LocalDateTime expireTime;
}
