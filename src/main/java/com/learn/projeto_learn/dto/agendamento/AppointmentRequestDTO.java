package com.learn.projeto_learn.dto.agendamento;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentRequestDTO(
        @NotNull(message = "O ID do paciente é obrigatório")
        UUID pacienteId,
        @NotNull(message = "A data e hora são obrigatórias")
        @Future(message = "O agendamento deve ser para o futuro")
        LocalDateTime dataHora
) {
}