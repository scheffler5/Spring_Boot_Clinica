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
        String cidade,
        String fotoUrl,
        BigDecimal valorConsulta,
        Integer duracaoConsultaMinutos
) {
    public MedicoMarketplaceDTO(Usuario m) {
        this(
                m.getId(),
                m.getNome() != null && !m.getNome().isBlank() ? m.getNome() : m.getLogin(),
                m.getEspecialidade() != null ? m.getEspecialidade().name() : null,
                m.getEspecialidade() != null ? m.getEspecialidade().getDescricao() : null,
                m.getCrm(),
                m.getCidade(),
                m.getFotoUrl(),
                m.getValorConsulta(),
                m.getDuracaoConsultaMinutos()
        );
    }
}
