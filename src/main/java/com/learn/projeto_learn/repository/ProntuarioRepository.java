package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.model.medicalrecord.Prontuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProntuarioRepository extends JpaRepository<Prontuario, UUID> {
    List<Prontuario> findAllByPacienteId(UUID pacienteId);
    List<Prontuario> findAllByMedicoId(UUID medicoId);
}
