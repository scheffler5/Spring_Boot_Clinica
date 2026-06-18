package com.learn.projeto_learn.service.Agendamento;

import com.learn.projeto_learn.dto.agendamento.AppointmentRequestDTO;
import com.learn.projeto_learn.dto.agendamento.AppointmentResponseDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.agendamento.Agendamento;
import com.learn.projeto_learn.model.patient.Paciente;
import com.learn.projeto_learn.repository.AgendamentoRepository;
import com.learn.projeto_learn.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AgendamentoService {

    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private PacienteRepository pacienteRepository;

    public Agendamento createAppointment(AppointmentRequestDTO data) {
        Paciente paciente = pacienteRepository.findById(data.pacienteId())
                .filter(p -> Boolean.TRUE.equals(p.getAtivo()))
                .orElseThrow(() -> new BusinessException("Paciente não encontrado.", HttpStatus.NOT_FOUND));

        if (agendamentoRepository.existsByDataHora(data.dataHora())) {
            throw new BusinessException("Já existe um agendamento neste horário.");
        }

        return agendamentoRepository.save(new Agendamento(paciente, data.dataHora()));
    }

    public List<AppointmentResponseDTO> listAll() {
        return agendamentoRepository.findAll().stream()
                .map(AppointmentResponseDTO::new)
                .toList();
    }

    public List<AppointmentResponseDTO> listByPaciente(UUID pacienteId) {
        return agendamentoRepository.findAllByPacienteId(pacienteId).stream()
                .map(AppointmentResponseDTO::new)
                .toList();
    }

    public AppointmentResponseDTO findById(UUID id) {
        return agendamentoRepository.findById(id)
                .map(AppointmentResponseDTO::new)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado.", HttpStatus.NOT_FOUND));
    }
}
