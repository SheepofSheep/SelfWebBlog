package org.example.selfwebblog.config;

import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.security.RequestTraceFilter;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class ResultHttpStatusAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
        Object body,
        MethodParameter returnType,
        MediaType selectedContentType,
        Class<? extends HttpMessageConverter<?>> selectedConverterType,
        ServerHttpRequest request,
        ServerHttpResponse response
    ) {
        if (body instanceof Result<?> result && result.getCode() != null) {
            String traceId = request.getHeaders().getFirst(RequestTraceFilter.HEADER);
            if (request instanceof ServletServerHttpRequest servletRequest) {
                Object generated = servletRequest.getServletRequest()
                        .getAttribute(RequestTraceFilter.ATTRIBUTE);
                if (generated instanceof String value && !value.isBlank()) traceId = value;
            }
            result.setTraceId(traceId);
            HttpStatus status = HttpStatus.resolve(result.getCode());
            if (status != null) {
                response.setStatusCode(status);
            }
            if (result.getCode() == Result.RATE_LIMITED && result.getRetryAfterSeconds() != null) {
                response.getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(result.getRetryAfterSeconds()));
            }
        }
        return body;
    }
}
