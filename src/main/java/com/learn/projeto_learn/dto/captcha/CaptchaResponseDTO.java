package com.learn.projeto_learn.dto.captcha;

public record CaptchaResponseDTO(String challengeId, String challenge, int difficulty) {}
