package com.learn.projeto_learn.dto.agendamento;

import com.learn.projeto_learn.model.User.Usuario;
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
        LocalDateTime dataHora,
        StatusAgendamento status,
        LocalDateTime createdAt
) {
    public AppointmentResponseDTO(Agendamento a) {
        this(a.getId(),
             a.getPaciente().getId(), a.getPaciente().getNome(),
             a.getMedico().getId(), nomeMedico(a.getMedico()),
             a.getDataHora(), a.getStatus(), a.getCreatedAt());
    }

    private static String nomeMedico(Usuario medico) {
        return (medico.getNome() != null && !medico.getNome().isBlank())
                ? medico.getNome()
                : medico.getLogin();
    }
}
