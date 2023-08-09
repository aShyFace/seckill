package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private Boolean success;
    private String errorMsg;
    private Object data;
    private Long total;
    private Integer code;


    public static Result ok(){
        return new Result(true, null, null, null, 200);
    }
    public static Result ok(AppHttpCodeEnum msg, Object data){
        String msgMsg = msg.getMsg();
        Integer code = msg.getCode();
        return new Result(false, msgMsg, data, null, code);
    }
    public static Result ok(Object data){
        return new Result(true, null, data, null, 200);
    }
    public static Result ok(List<?> data, Long total){
        return new Result(true, null, data, total, 200);
    }
    public static Result fail(String errorMsg){
        return new Result(false, errorMsg, null, null, 404);
    }
    public static Result fail(Integer code, String errorMsg){
        return new Result(false, errorMsg, null, null, code);
    }
    public static Result fail(AppHttpCodeEnum msg){
        String msgMsg = msg.getMsg();
        Integer code = msg.getCode();
        return new Result(false, msgMsg, null, null, code);
    }
}
