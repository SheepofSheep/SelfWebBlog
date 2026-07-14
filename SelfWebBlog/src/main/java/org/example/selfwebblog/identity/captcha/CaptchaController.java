package org.example.selfwebblog.identity.captcha;

import org.example.selfwebblog.entity.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {
    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping
    public Result<CaptchaResponse> create(@RequestParam String purpose) {
        return Result.success(captchaService.create(purpose));
    }
}
