package com.learn.projeto_learn.dto.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatientRegisterDTO(
        @NotBlank(message = "Login é obrigatório")
        @Size(min = 3, max = 50, message = "Login deve ter entre 3 e 50 caracteres")
        String login,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 100, message = "A senha deve ter no mínimo 8 caracteres")
        String password,

        @NotBlank(message = "CAPTCHA ID é obrigatório")
        String captchaId,

        @NotBlank(message = "Código CAPTCHA é obrigatório")
        String captchaCode
) {}
