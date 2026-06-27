package com.learn.projeto_learn.dto.agendamento;

import com.learn.projeto_learn.model.user.Usuario;
import com.learn.projeto_learn.model.agendamento.Agendamento;
import com.learn.projeto_learn.model.agendamento.StatusAgendamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponseDTO(
        UUID id,
        UUID pacienteId,
        String nomePaciente,
        UUID medicoId,
        String nomeMedico,
        String fotoMedicoUrl,
        LocalDateTime dataHora,
        StatusAgendamento status,
        LocalDateTime createdAt
) {
    public AppointmentResponseDTO(Agendamento a) {
        this(
            a.getId(),
            a.getPaciente().getId(),
            a.getPaciente().getNome(),
            a.getMedico().getId(),
            nomeMedico(a.getMedico()),
            a.getMedico().getFotoUrl(),
            a.getDataHora(),
            a.getStatus(),
            a.getCreatedAt()
        );
    }

    private static String nomeMedico(Usuario m) {
        return (m.getNome() != null && !m.getNome().isBlank()) ? m.getNome() : m.getLogin();
    }
}
