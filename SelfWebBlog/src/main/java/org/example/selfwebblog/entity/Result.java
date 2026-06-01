package org.example.selfwebblog.entity;

import lombok.Data;

@Data // 自动生成 get/set 方法
public class Result<T> {
    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int RATE_LIMITED = 429;
    public static final int SERVER_ERROR = 500;

    private Integer code; // 业务码：与 HTTP 状态保持一致
    private String msg;   // 给用户看的提示词
    private T data;       // 真正给前端的数据（比如博客列表）

    // 快捷返回成功的方法（带数据）
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = SUCCESS;
        r.msg = "操作成功";
        r.data = data;
        return r;
    }
    
    // 快捷返回成功的方法（无数据）
    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.code = SUCCESS;
        r.msg = "操作成功";
        r.data = null;
        return r;
    }

    // 快捷返回失败的方法
    public static <T> Result<T> error(String msg) {
        return of(SERVER_ERROR, msg, null);
    }

    public static <T> Result<T> badRequest(String msg) {
        return of(BAD_REQUEST, msg, null);
    }

    public static <T> Result<T> unauthorized(String msg) {
        return of(UNAUTHORIZED, msg, null);
    }

    public static <T> Result<T> forbidden(String msg) {
        return of(FORBIDDEN, msg, null);
    }

    public static <T> Result<T> notFound(String msg) {
        return of(NOT_FOUND, msg, null);
    }

    public static <T> Result<T> rateLimited(String msg) {
        return of(RATE_LIMITED, msg, null);
    }

    public static <T> Result<T> serverError(String msg) {
        return of(SERVER_ERROR, msg, null);
    }

    public static <T> Result<T> of(int code, String msg, T data) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        r.data = data;
        return r;
    }
}
