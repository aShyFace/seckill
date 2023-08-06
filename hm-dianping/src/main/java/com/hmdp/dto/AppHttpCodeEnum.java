package com.hmdp.dto;

import lombok.Data;


public enum AppHttpCodeEnum {
    // 成功
    SUCCESS(200,"操作成功"),


    // 登录
    NEED_LOGIN(401,"需要登录后操作"),
    NO_OPERATOR_AUTH(403,"无权限操作"),
    PARAM_IS_NULL(405, "参数不能为空"),
    PARAM_INVALID(406, "非法参数"),
    FILE_TYPE_ERROR(407, "文件类型错误"),
    SECKILL_FAILD(408, "抢光了"),
    ORDER_FAILD(409, "只能抢一次"),


    SYSTEM_ERROR(500,"出现错误"),
    PHONENUMBER_EXIST(502,"手机号已存在"),
    EMAIL_EXIST(503, "邮箱已存在"),
    REQUIRE_USERNAME(504, "必需填写用户名"),
    LOGIN_ERROR(505,"用户名或密码错误"),
    FILE_UPLOAD_ERROR(506, "上传出错"),
    RESPONSE_ANALYSIS_ERROR(507, "响应解析出错"),
    USER_NOT_EXIST(508, "用户不存在"),
    USERNAME_EXIST(509,"用户名已存在"),
    NICKNAME_EXIST(510,"昵称已存在"),

    DELETE_ERROR(596, "删除数据失败"),
    UPDATE_ERROR(597, "修改数据失败"),
    QUERY_ERROR(598, "查询数据失败"),
    INSTER_ERROR(599, "添加数据失败"),
    ;
    int code;
    String msg;

    AppHttpCodeEnum(int code, String errorMessage){
        this.code = code;
        this.msg = errorMessage;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
    //public int setCode() {
    //    return code;
    //}
    //
    //public String setMsg() {
    //    return msg;
    //}
}