package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.model.agendamento.Agendamento;
import com.learn.projeto_learn.model.agendamento.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AgendamentoRepository extends JpaRepository<Agendamento, UUID> {

    boolean existsByMedicoIdAndDataHora(UUID medicoId, LocalDateTime dataHora);

    List<Agendamento> findAllByPacienteId(UUID pacienteId);

    List<Agendamento> findAllByMedicoId(UUID medicoId);

    List<Agendamento> findAllByMedicoIdAndDataHoraBetween(UUID medicoId, LocalDateTime inicio, LocalDateTime fim);

    List<Agendamento> findAllByMedicoIdAndStatusAndDataHoraBetween(
            UUID medicoId, StatusAgendamento status, LocalDateTime inicio, LocalDateTime fim);

    List<Agendamento> findAllByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);
    List<Agendamento> findAllByMedicoIdAndDataHoraBetween(UUID medicoId, LocalDateTime inicio, LocalDateTime fim);
}
