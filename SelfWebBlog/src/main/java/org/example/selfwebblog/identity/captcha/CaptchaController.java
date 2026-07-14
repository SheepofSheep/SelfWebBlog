package org.example.selfwebblog.identity.captcha;

import jakarta.servlet.http.HttpServletRequest;
import org.example.selfwebblog.entity.Result;
import org.example.selfwebblog.security.ActionRateLimiter;
import org.example.selfwebblog.security.ClientIpResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {
    private final CaptchaService captchaService;
    private final ActionRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    public CaptchaController(
            CaptchaService captchaService,
            ActionRateLimiter rateLimiter,
            ClientIpResolver clientIpResolver) {
        this.captchaService = captchaService;
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping
    public Result<CaptchaResponse> create(@RequestParam String purpose, HttpServletRequest request) {
        ActionRateLimiter.Decision decision = rateLimiter.acquire(
                "captcha:" + purpose,
                clientIpResolver.resolve(request),
                20,
                Duration.ofMinutes(10));
        if (!decision.allowed()) {
            return Result.rateLimited("验证码请求过于频繁，请稍后再试", decision.retryAfterSeconds());
        }
        return Result.success(captchaService.create(purpose));
    }
}
