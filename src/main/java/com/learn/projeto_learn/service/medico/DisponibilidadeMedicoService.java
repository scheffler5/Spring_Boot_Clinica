package com.learn.projeto_learn.service.medico;

import com.learn.projeto_learn.dto.medico.DisponibilidadeRequestDTO;
import com.learn.projeto_learn.dto.medico.DisponibilidadeResponseDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.agendamento.Agendamento;
import com.learn.projeto_learn.model.agendamento.DisponibilidadeMedico;
import com.learn.projeto_learn.model.agendamento.StatusAgendamento;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.repository.AgendamentoRepository;
import com.learn.projeto_learn.repository.DisponibilidadeMedicoRepository;
import com.learn.projeto_learn.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DisponibilidadeMedicoService {

    @Autowired private DisponibilidadeMedicoRepository disponibilidadeRepository;
    @Autowired private UsuarioRepository               usuarioRepository;
    @Autowired private AgendamentoRepository           agendamentoRepository;

    @Transactional
    public DisponibilidadeResponseDTO adicionar(UUID medicoId, DisponibilidadeRequestDTO data) {
        if (!data.horaFim().isAfter(data.horaInicio())) {
            throw new BusinessException("Hora de fim deve ser posterior à hora de início.");
        }

        Usuario medico = usuarioRepository.findById(medicoId)
                .orElseThrow(() -> new BusinessException("Médico não encontrado.", HttpStatus.NOT_FOUND));

        return new DisponibilidadeResponseDTO(
                disponibilidadeRepository.save(
                        new DisponibilidadeMedico(medico, data.diaSemana(), data.horaInicio(), data.horaFim())));
    }

    public List<DisponibilidadeResponseDTO> listar(UUID medicoId) {
        return disponibilidadeRepository.findAllByMedicoIdAndAtivoTrue(medicoId).stream()
                .map(DisponibilidadeResponseDTO::new)
                .toList();
    }

    @Transactional
    public void remover(UUID medicoId, UUID disponibilidadeId) {
        DisponibilidadeMedico d = disponibilidadeRepository.findById(disponibilidadeId)
                .orElseThrow(() -> new BusinessException("Disponibilidade não encontrada.", HttpStatus.NOT_FOUND));
        if (!d.getMedico().getId().equals(medicoId)) {
            throw new BusinessException("Disponibilidade não pertence a este médico.", HttpStatus.FORBIDDEN);
        }
        d.setAtivo(false);
        disponibilidadeRepository.save(d);
    }

    public List<String> gerarSlotsDisponiveis(UUID medicoId, LocalDate data) {
        Usuario medico = usuarioRepository.findById(medicoId)
                .orElseThrow(() -> new BusinessException("Médico não encontrado.", HttpStatus.NOT_FOUND));

        int duracao = medico.getDuracaoConsultaMinutos() != null ? medico.getDuracaoConsultaMinutos() : 60;

        DayOfWeek diaSemana = data.getDayOfWeek();
        List<DisponibilidadeMedico> disponibilidades =
                disponibilidadeRepository.findAllByMedicoIdAndDiaSemanaAndAtivoTrue(medicoId, diaSemana);

        if (disponibilidades.isEmpty()) return List.of();

        Set<LocalDateTime> ocupados = agendamentoRepository
                .findAllByMedicoIdAndDataHoraBetween(medicoId, data.atStartOfDay(), data.atTime(23, 59, 59)).stream()
                .filter(a -> a.getStatus() != StatusAgendamento.CANCELADO)
                .map(Agendamento::getDataHora)
                .collect(Collectors.toSet());

        List<String> slots = new ArrayList<>();

        for (DisponibilidadeMedico disp : disponibilidades) {
            LocalTime limite = disp.getHoraFim().minus(duracao, ChronoUnit.MINUTES);
            LocalTime cursor = disp.getHoraInicio();
            while (!cursor.isAfter(limite)) {
                LocalDateTime slot = data.atTime(cursor);
                if (!ocupados.contains(slot) && slot.isAfter(LocalDateTime.now())) {
                    slots.add(slot.toString());
                }
                cursor = cursor.plus(duracao, ChronoUnit.MINUTES);
            }
        }
        return slots;
    }
}
