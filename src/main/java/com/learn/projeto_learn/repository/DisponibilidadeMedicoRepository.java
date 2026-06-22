package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.model.agendamento.DisponibilidadeMedico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface DisponibilidadeMedicoRepository extends JpaRepository<DisponibilidadeMedico, UUID> {

    List<DisponibilidadeMedico> findAllByMedicoIdAndAtivoTrue(UUID medicoId);

    List<DisponibilidadeMedico> findAllByMedicoIdAndDiaSemanaAndAtivoTrue(UUID medicoId, DayOfWeek diaSemana);

    boolean existsByMedicoIdAndDiaSemana(UUID medicoId, DayOfWeek diaSemana);
}
