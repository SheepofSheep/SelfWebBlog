package org.example.selfwebblog.identity.captcha;

public record CaptchaResponse(String challengeId, String imageDataUrl) {
}
