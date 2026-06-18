package com.learn.projeto_learn.dto.Login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerifyDTO(
        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "Código de verificação é obrigatório")
        String code
) {}
