package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.model.agendamento.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AgendamentoRepository extends JpaRepository<Agendamento, UUID> {
    boolean existsByDataHora(LocalDateTime dataHora);
    List<Agendamento> findAllByPacienteId(UUID pacienteId);
    List<Agendamento> findAllByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);
}
