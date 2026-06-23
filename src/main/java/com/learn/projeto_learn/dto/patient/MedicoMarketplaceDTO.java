package com.learn.projeto_learn.dto.patient;

import com.learn.projeto_learn.model.User.Usuario;

import java.math.BigDecimal;
import java.util.UUID;

public record MedicoMarketplaceDTO(
        UUID id,
        String nome,
        String especialidade,
        String especialidadeDescricao,
        String crm,
        BigDecimal valorConsulta
) {
    public MedicoMarketplaceDTO(Usuario m) {
        this(
                m.getId(),
                (m.getNome() != null && !m.getNome().isBlank()) ? m.getNome() : m.getLogin(),
                m.getEspecialidade() != null ? m.getEspecialidade().name() : null,
                m.getEspecialidade() != null ? m.getEspecialidade().getDescricao() : null,
                m.getCrm(),
                m.getValorConsulta()
        );
    }
}
