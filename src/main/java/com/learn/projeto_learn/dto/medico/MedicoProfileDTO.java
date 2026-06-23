package com.learn.projeto_learn.dto.medico;

import com.learn.projeto_learn.model.User.Especialidade;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record MedicoProfileDTO(
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 100)
        String nome,

        @NotBlank(message = "CRM é obrigatório")
        @Size(max = 20)
        String crm,

        @NotNull(message = "Especialidade é obrigatória")
        Especialidade especialidade,

        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
        String cidade,

        @NotNull(message = "Valor da consulta é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal valorConsulta,

        @NotNull(message = "Duração da consulta é obrigatória")
        @Min(value = 15, message = "Duração mínima é 15 minutos")
        @Max(value = 120, message = "Duração máxima é 120 minutos")
        Integer duracaoConsultaMinutos,

        @Size(max = 2000, message = "Descrição deve ter no máximo 2000 caracteres")
        String descricao,

        @Size(max = 150, message = "Universidade deve ter no máximo 150 caracteres")
        String universidade,

        @Min(value = 1950, message = "Ano inválido")
        Integer anoFormacao
) {}
