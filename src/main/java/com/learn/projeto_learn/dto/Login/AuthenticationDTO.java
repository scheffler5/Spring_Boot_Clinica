package com.learn.projeto_learn.dto.Login;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationDTO(
        @NotBlank(message = "Login é obrigatório") String login,
        @NotBlank(message = "Senha é obrigatória") String password,
        @NotBlank(message = "CAPTCHA ID é obrigatório") String captchaId,
        @NotBlank(message = "Código CAPTCHA é obrigatório") String captchaCode
) {}
