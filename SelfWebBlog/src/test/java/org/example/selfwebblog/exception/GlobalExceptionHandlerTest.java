package org.example.selfwebblog.exception;

import org.example.selfwebblog.entity.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    @Test
    void mapsUnknownResourcesToNotFound() {
        Result<String> result = new GlobalExceptionHandler().handleNotFound(
                new NoResourceFoundException(HttpMethod.GET, "/api/missing"));

        assertEquals(Result.NOT_FOUND, result.getCode());
        assertEquals("请求的资源不存在", result.getMsg());
    }

    @Test
    void mapsMissingParametersToBadRequest() {
        Result<String> result = new GlobalExceptionHandler().handleMissingParameter(
                new MissingServletRequestParameterException("path", "String"));

        assertEquals(Result.BAD_REQUEST, result.getCode());
        assertEquals("缺少请求参数：path", result.getMsg());
    }
}
