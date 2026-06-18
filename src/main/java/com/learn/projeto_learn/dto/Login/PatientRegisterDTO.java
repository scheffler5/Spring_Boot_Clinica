package com.learn.projeto_learn.dto.Login;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

public record PatientRegisterDTO(
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
        String nome,

        @NotBlank(message = "CPF é obrigatório")
        @CPF(message = "CPF inválido")
        String cpf,

        @NotNull(message = "Data de nascimento é obrigatória")
        @Past(message = "Data de nascimento deve ser no passado")
        LocalDate dataNascimento,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 100, message = "A senha deve ter no mínimo 8 caracteres")
        String password,

        @NotBlank(message = "CAPTCHA ID é obrigatório")
        String captchaId,

        @NotBlank(message = "Código CAPTCHA é obrigatório")
        String captchaCode
) {}
