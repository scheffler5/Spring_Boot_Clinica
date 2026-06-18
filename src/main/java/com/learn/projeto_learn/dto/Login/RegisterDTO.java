package com.learn.projeto_learn.dto.Login;

import com.learn.projeto_learn.model.User.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDTO(
        @NotBlank(message = "Login é obrigatório")
        @Size(min = 3, max = 50, message = "Login deve ter entre 3 e 50 caracteres")
        String login,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 100, message = "A senha deve ter no mínimo 8 caracteres")
        String password,

        @NotNull(message = "Role é obrigatório")
        UserRole role,

        @NotBlank(message = "CAPTCHA ID é obrigatório")
        String captchaId,

        @NotBlank(message = "Código CAPTCHA é obrigatório")
        String captchaCode
) {}
