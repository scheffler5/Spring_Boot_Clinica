package com.learn.projeto_learn.dto.medicalrecord;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProntuarioRequestDTO(
        @NotNull UUID idPaciente,
        @NotNull UUID idConvenio,
        @NotNull UUID idProcedimento,
        @NotNull UUID idMedico,

        String observacoes
) {
}
