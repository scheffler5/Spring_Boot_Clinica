package com.learn.projeto_learn.dto.paciente;

import com.learn.projeto_learn.Domain.patient.Paciente;

import java.util.UUID;

public record PatientResponseDTO(
        UUID id,
        String nome,
        String cpf
) {
    // Construtor para converter Entidade -> DTO
    public PatientResponseDTO(Paciente paciente) {
        this(paciente.getId(), paciente.getNome(), paciente.getCpf());
    }
}