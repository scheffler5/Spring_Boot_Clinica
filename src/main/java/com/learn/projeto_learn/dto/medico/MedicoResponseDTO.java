package com.learn.projeto_learn.dto.medico;

import com.learn.projeto_learn.model.User.Especialidade;
import com.learn.projeto_learn.model.User.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MedicoResponseDTO(
        UUID id,
        String nome,
        String crm,
        Especialidade especialidade,
        String descricaoEspecialidade,
        String cidade,
        String fotoUrl,
        BigDecimal valorConsulta,
        Integer duracaoConsultaMinutos,
        String descricao,
        String universidade,
        Integer anoFormacao,
        Integer anosExperiencia,
        boolean perfilCompleto
) {
    public MedicoResponseDTO(Usuario u) {
        this(
                u.getId(),
                u.getNome(),
                u.getCrm(),
                u.getEspecialidade(),
                u.getEspecialidade() != null ? u.getEspecialidade().getDescricao() : null,
                u.getCidade(),
                u.getFotoUrl(),
                u.getValorConsulta(),
                u.getDuracaoConsultaMinutos(),
                u.getDescricao(),
                u.getUniversidade(),
                u.getAnoFormacao(),
                u.getAnoFormacao() != null ? LocalDate.now().getYear() - u.getAnoFormacao() : null,
                Boolean.TRUE.equals(u.getPerfilCompleto())
        );
    }
}
