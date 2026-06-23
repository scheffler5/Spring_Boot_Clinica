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
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class AgendamentoService {

    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private PacienteRepository    pacienteRepository;
    @Autowired private UsuarioRepository     usuarioRepository;

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

    @Transactional
    public AppointmentResponseDTO cancelar(UUID agendamentoId, Usuario solicitante) {
        Agendamento a = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado.", HttpStatus.NOT_FOUND));

        if (a.getStatus() == StatusAgendamento.CANCELADO) {
            throw new BusinessException("Agendamento já está cancelado.");
        }
        if (a.getStatus() == StatusAgendamento.REALIZADO) {
            throw new BusinessException("Não é possível cancelar uma consulta já realizada.");
        }

        LocalDateTime agora   = LocalDateTime.now();
        long horasRestantes   = ChronoUnit.HOURS.between(agora, a.getDataHora());
        UserRole role         = solicitante.getRole();

        if (role == UserRole.PACIENTE) {
            if (solicitante.getPaciente() == null ||
                !a.getPaciente().getId().equals(solicitante.getPaciente().getId())) {
                throw new BusinessException("Agendamento não pertence a este paciente.", HttpStatus.FORBIDDEN);
            }
            if (horasRestantes < 24) {
                throw new BusinessException(
                    "Cancelamento pelo paciente requer ao menos 24h de antecedência. " +
                    "Restam " + horasRestantes + "h para a consulta.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else if (role == UserRole.MEDIC || role == UserRole.ADMIN) {
            if (!a.getMedico().getId().equals(solicitante.getId())) {
                throw new BusinessException("Agendamento não pertence a este médico.", HttpStatus.FORBIDDEN);
            }
            if (horasRestantes < 10) {
                throw new BusinessException(
                    "Cancelamento pelo médico requer ao menos 10h de antecedência. " +
                    "Restam " + horasRestantes + "h para a consulta.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else {
            throw new BusinessException("Não autorizado.", HttpStatus.FORBIDDEN);
        }

        a.setStatus(StatusAgendamento.CANCELADO);
        return new AppointmentResponseDTO(agendamentoRepository.save(a));
    }

    public AppointmentResponseDTO updateStatus(UUID id, StatusAgendamento novoStatus) {
        Agendamento a = agendamentoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado.", HttpStatus.NOT_FOUND));
        a.setStatus(novoStatus);
        return new AppointmentResponseDTO(agendamentoRepository.save(a));
    }

    public List<AppointmentResponseDTO> listAll() {
        return agendamentoRepository.findAll().stream().map(AppointmentResponseDTO::new).toList();
    }

    public List<AppointmentResponseDTO> listByPaciente(UUID pacienteId) {
        return agendamentoRepository.findAllByPacienteId(pacienteId).stream()
                .map(AppointmentResponseDTO::new).toList();
    }

    public AppointmentResponseDTO findById(UUID id) {
        return agendamentoRepository.findById(id)
                .map(AppointmentResponseDTO::new)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado.", HttpStatus.NOT_FOUND));
    }
}
