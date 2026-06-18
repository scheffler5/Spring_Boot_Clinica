package com.learn.projeto_learn.dto.agendamento;

import com.learn.projeto_learn.model.agendamento.Agendamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponseDTO(
        UUID id,
        UUID pacienteId,
        String nomePaciente,
        LocalDateTime dataHora,
        LocalDateTime createdAt
) {
    public AppointmentResponseDTO(Agendamento a) {
        this(a.getId(), a.getPaciente().getId(), a.getPaciente().getNome(), a.getDataHora(), a.getCreatedAt());
    }
}
