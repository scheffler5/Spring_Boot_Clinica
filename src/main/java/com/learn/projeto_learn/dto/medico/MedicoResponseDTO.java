package com.learn.projeto_learn.dto.medico;

import com.learn.projeto_learn.model.User.Especialidade;
import com.learn.projeto_learn.model.User.Usuario;

import java.math.BigDecimal;
import java.util.UUID;

public record MedicoResponseDTO(
        UUID id,
        String nome,
        String crm,
        Especialidade especialidade,
        String descricaoEspecialidade,
        BigDecimal valorConsulta,
        Integer duracaoConsultaMinutos,
        boolean perfilCompleto
) {
    public MedicoResponseDTO(Usuario u) {
        this(
                u.getId(),
                u.getNome(),
                u.getCrm(),
                u.getEspecialidade(),
                u.getEspecialidade() != null ? u.getEspecialidade().getDescricao() : null,
                u.getValorConsulta(),
                u.getDuracaoConsultaMinutos(),
                Boolean.TRUE.equals(u.getPerfilCompleto())
        );
    }
}
