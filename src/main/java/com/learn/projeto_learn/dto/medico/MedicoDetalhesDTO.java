package com.learn.projeto_learn.dto.medico;

import com.learn.projeto_learn.model.User.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MedicoDetalhesDTO(
        UUID id,
        String nome,
        String especialidade,
        String descricaoEspecialidade,
        String crm,
        String cidade,
        String fotoUrl,
        BigDecimal valorConsulta,
        Integer duracaoConsultaMinutos,
        String descricao,
        String universidade,
        Integer anoFormacao,
        Integer anosExperiencia,
        List<DisponibilidadeResponseDTO> disponibilidade
) {
    public MedicoDetalhesDTO(Usuario u, List<DisponibilidadeResponseDTO> disp) {
        this(
            u.getId(),
            u.getNome(),
            u.getEspecialidade() != null ? u.getEspecialidade().name() : null,
            u.getEspecialidade() != null ? u.getEspecialidade().getDescricao() : null,
            u.getCrm(),
            u.getCidade(),
            u.getFotoUrl(),
            u.getValorConsulta(),
            u.getDuracaoConsultaMinutos(),
            u.getDescricao(),
            u.getUniversidade(),
            u.getAnoFormacao(),
            u.getAnoFormacao() != null ? LocalDate.now().getYear() - u.getAnoFormacao() : null,
            disp
        );
    }
}
