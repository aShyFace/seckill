package com.hmdp.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;


@Slf4j
public class JsonUtil {
  public static ObjectMapper mapper = JsonMapper.builder()
    // 反斜杠
    .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
    // 值为正无穷、负无穷或NaN时，默认反序列化会失败
    .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
    //开启JSON字符串包含非引号控制字符的解析（\n换行符）
    .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
    // 转换为格式化的json
    .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
    .build();

  static {
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      // 1.如果json中有新增的字段并且是实体类类中不存在的，不报错
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      // 2.属性为NULL不被序列化
      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      // 3.序列化没有属性的空对象时会抛异常
      mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
      // 注释
      mapper.configure(Feature.ALLOW_COMMENTS, true);
      // 需要开启单引号解析属性，默认是false
      mapper.configure(Feature.ALLOW_SINGLE_QUOTES, true);
      //取消时间的转化格式默认是时间戳,可以取消,同时需要设置要表现的时间格式
      mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
      mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
      mapper.registerModule(new JavaTimeModule());
  }

  public static String object2Json(final Object data) {
    try {
      return mapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T json2Object(final String json, Class<T> clazz) {
    try {
      return mapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> T json2Object(final String json, TypeReference<T> type) {
    try {
      return mapper.readValue(json, type);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

}
