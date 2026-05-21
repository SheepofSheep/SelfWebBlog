package org.example.selfwebblog.entity;

import lombok.Data;

@Data // 自动生成 get/set 方法
public class Result<T> {
    private Integer code; // 状态码：200成功，500失败
    private String msg;   // 给用户看的提示词
    private T data;       // 真正给前端的数据（比如博客列表）

    // 快捷返回成功的方法（带数据）
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "操作成功";
        r.data = data;
        return r;
    }
    
    // 快捷返回成功的方法（无数据）
    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "操作成功";
        r.data = null;
        return r;
    }

    // 快捷返回失败的方法
    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.code = 500;
        r.msg = msg;
        r.data = null;
        return r;
    }
}