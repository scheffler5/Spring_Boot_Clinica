package com.learn.projeto_learn.dto.medicalrecord;

import com.learn.projeto_learn.model.medicalrecord.Prontuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProntuarioResponseDTO(
        UUID id,
        String nomePaciente,
        String nomeMedico,
        String nomeConvenio,
        String descricaoProcedimento,
        BigDecimal valorCalculado,
        String observacoes,
        LocalDateTime dataAtendimento
) {
    public ProntuarioResponseDTO(Prontuario p) {
        this(
                p.getId(),
                p.getPaciente().getNome(),
                p.getMedico().getLogin(),
                p.getConvenio().getNome(),
                p.getProcedimento().getDescricao(),
                p.getValorCalculado(),
                p.getObservacoes(),
                p.getDataAtendimento()
        );
    }
}
