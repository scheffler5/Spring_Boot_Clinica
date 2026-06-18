package com.learn.projeto_learn.dto.procedimento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProcedimentoRequestDTO(
        @NotBlank(message = "Descrição do procedimento é obrigatória")
        String descricao,

        @NotNull(message = "Custo é obrigatório")
        @DecimalMin(value = "0.01", message = "Custo deve ser maior que zero")
        BigDecimal custo
) {}
