package com.learn.projeto_learn.dto.agendamento;

import com.learn.projeto_learn.model.agendamento.StatusAgendamento;
import jakarta.validation.constraints.NotNull;

public record AppointmentStatusDTO(
        @NotNull(message = "O status é obrigatório")
        StatusAgendamento status
) {
}
