package com.learn.projeto_learn.dto.convenio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConvenioRequestDTO(
        @NotBlank(message = "Nome do convênio é obrigatório")
        String nome,

        @NotNull(message = "Desconto é obrigatório")
        @DecimalMin(value = "0.0", inclusive = true, message = "Desconto não pode ser negativo")
        BigDecimal desconto
) {}
