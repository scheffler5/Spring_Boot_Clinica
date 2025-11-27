package com.learn.projeto_learn.repository;


import com.learn.projeto_learn.Domain.patient.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PacienteRepository extends JpaRepository<Paciente, UUID> {
    boolean existsByCpf(String cpf);
    List<Paciente> findByNomeContainingIgnoreCaseOrCpf(String nome, String cpf);
}