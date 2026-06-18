package com.learn.projeto_learn.dto.convenio;

import com.learn.projeto_learn.model.insurance.Convenio;

import java.math.BigDecimal;
import java.util.UUID;

public record ConvenioResponseDTO(UUID id, String nome, BigDecimal desconto, Boolean ativo) {
    public ConvenioResponseDTO(Convenio c) {
        this(c.getId(), c.getNome(), c.getDesconto(), c.getAtivo());
    }
}
