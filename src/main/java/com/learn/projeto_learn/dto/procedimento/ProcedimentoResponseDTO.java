package com.learn.projeto_learn.dto.procedimento;

import com.learn.projeto_learn.model.procedure.Procedimento;

import java.math.BigDecimal;
import java.util.UUID;

public record ProcedimentoResponseDTO(UUID id, String descricao, BigDecimal custo, Boolean ativo) {
    public ProcedimentoResponseDTO(Procedimento p) {
        this(p.getId(), p.getDescricao(), p.getCusto(), p.getAtivo());
    }
}
