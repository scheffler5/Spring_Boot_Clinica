package com.learn.projeto_learn.dto.patient;

import com.learn.projeto_learn.model.patient.Paciente;

import java.time.LocalDate;
import java.util.UUID;

public record PatientResponseDTO(UUID id, String nome, String cpf, LocalDate dataNascimento, Boolean ativo, String fotoUrl) {
    public PatientResponseDTO(Paciente p) {
        this(p.getId(), p.getNome(), p.getCpf(), p.getDataNascimento(), p.getAtivo(), null);
    }

    public PatientResponseDTO(Paciente p, String fotoUrl) {
        this(p.getId(), p.getNome(), p.getCpf(), p.getDataNascimento(), p.getAtivo(), fotoUrl);
    }
}
