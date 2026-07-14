package org.example.selfwebblog.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.security.RequestTraceFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return Result.badRequest(msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<String> handleIllegalArgument(IllegalArgumentException e) {
        return Result.badRequest(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<String> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.badRequest("请求参数类型不正确：" + e.getName());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<String> handleMissingParameter(MissingServletRequestParameterException e) {
        return Result.badRequest("缺少请求参数：" + e.getParameterName());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<String> handleNotFound(NoResourceFoundException e) {
        return Result.notFound("请求的资源不存在");
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 requestId={}", request.getAttribute(RequestTraceFilter.ATTRIBUTE), e);
        return Result.serverError("服务器内部错误，请稍后重试");
    }
}
