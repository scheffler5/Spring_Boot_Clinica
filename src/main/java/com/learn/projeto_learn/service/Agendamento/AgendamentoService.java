package com.learn.projeto_learn.service.Agendamento;

import com.learn.projeto_learn.dto.agendamento.AppointmentRequestDTO;
import com.learn.projeto_learn.dto.agendamento.AppointmentResponseDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.User.UserRole;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.model.agendamento.Agendamento;
import com.learn.projeto_learn.model.agendamento.StatusAgendamento;
import com.learn.projeto_learn.model.patient.Paciente;
import com.learn.projeto_learn.repository.AgendamentoRepository;
import com.learn.projeto_learn.repository.PacienteRepository;
import com.learn.projeto_learn.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AgendamentoService {

    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private PacienteRepository pacienteRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    public Agendamento createAppointment(AppointmentRequestDTO data) {
        Paciente paciente = pacienteRepository.findById(data.pacienteId())
                .filter(p -> Boolean.TRUE.equals(p.getAtivo()))
                .orElseThrow(() -> new BusinessException("Paciente não encontrado.", HttpStatus.NOT_FOUND));

        Usuario medico = usuarioRepository.findById(data.medicoId())
                .orElseThrow(() -> new BusinessException("Médico não encontrado.", HttpStatus.NOT_FOUND));

        if (medico.getRole() != UserRole.MEDIC && medico.getRole() != UserRole.ADMIN) {
            throw new BusinessException("O usuário informado não é um médico.");
        }
        if (!Boolean.TRUE.equals(medico.getAtivo())) {
            throw new BusinessException("O médico informado não está ativo.");
        }

        if (agendamentoRepository.existsByMedicoIdAndDataHora(medico.getId(), data.dataHora())) {
            throw new BusinessException("Este médico já possui um agendamento neste horário.");
        }

        return agendamentoRepository.save(new Agendamento(paciente, medico, data.dataHora()));
    }

    public AppointmentResponseDTO updateStatus(UUID id, StatusAgendamento novoStatus) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado.", HttpStatus.NOT_FOUND));
        agendamento.setStatus(novoStatus);
        return new AppointmentResponseDTO(agendamentoRepository.save(agendamento));
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
