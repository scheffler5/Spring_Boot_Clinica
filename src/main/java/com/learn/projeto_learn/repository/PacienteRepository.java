package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.model.patient.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PacienteRepository extends JpaRepository<Paciente, UUID> {
    boolean existsByCpf(String cpf);
    Optional<Paciente> findByCpf(String cpf);
    List<Paciente> findAllByAtivo(Boolean ativo);
    List<Paciente> findByNomeContainingIgnoreCaseOrCpfAndAtivo(String nome, String cpf, Boolean ativo);
}
