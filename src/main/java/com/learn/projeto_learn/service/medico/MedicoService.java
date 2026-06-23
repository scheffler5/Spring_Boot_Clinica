package com.learn.projeto_learn.service.medico;

import com.learn.projeto_learn.dto.medico.MedicoEstatisticasDTO;
import com.learn.projeto_learn.dto.medico.MedicoProfileDTO;
import com.learn.projeto_learn.dto.medico.MedicoResponseDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.User.UserRole;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.model.agendamento.StatusAgendamento;
import com.learn.projeto_learn.repository.AgendamentoRepository;
import com.learn.projeto_learn.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class MedicoService {

    @Autowired private UsuarioRepository    usuarioRepository;
    @Autowired private AgendamentoRepository agendamentoRepository;

    @Transactional
    public MedicoResponseDTO completarPerfil(UUID medicoId, MedicoProfileDTO data) {
        Usuario medico = buscarMedico(medicoId);
        medico.setNome(data.nome());
        medico.setCrm(data.crm());
        medico.setEspecialidade(data.especialidade());
        medico.setValorConsulta(data.valorConsulta());
        medico.setDuracaoConsultaMinutos(data.duracaoConsultaMinutos());
        medico.setPerfilCompleto(true);
        return new MedicoResponseDTO(usuarioRepository.save(medico));
    }

    public MedicoResponseDTO buscarPerfil(UUID medicoId) {
        return new MedicoResponseDTO(buscarMedico(medicoId));
    }

    public MedicoEstatisticasDTO getEstatisticas(UUID medicoId, YearMonth mes) {
        LocalDateTime inicio = mes.atDay(1).atStartOfDay();
        LocalDateTime fim    = mes.atEndOfMonth().atTime(23, 59, 59);

        long atendidos = agendamentoRepository
                .findAllByMedicoIdAndStatusAndDataHoraBetween(medicoId, StatusAgendamento.REALIZADO, inicio, fim)
                .size();

        long agendados = agendamentoRepository
                .findAllByMedicoIdAndDataHoraBetween(medicoId, LocalDateTime.now(), fim).stream()
                .filter(a -> a.getStatus() == StatusAgendamento.AGENDADO
                          || a.getStatus() == StatusAgendamento.CONFIRMADO)
                .count();

        BigDecimal valor = buscarMedico(medicoId).getValorConsulta();
        if (valor == null) valor = BigDecimal.ZERO;

        return new MedicoEstatisticasDTO(
                (int) atendidos, (int) agendados,
                valor.multiply(BigDecimal.valueOf(atendidos)),
                valor.multiply(BigDecimal.valueOf(agendados))
        );
    }

    private Usuario buscarMedico(UUID id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Médico não encontrado.", HttpStatus.NOT_FOUND));
        if (u.getRole() != UserRole.MEDIC && u.getRole() != UserRole.ADMIN) {
            throw new BusinessException("Usuário não é médico.", HttpStatus.FORBIDDEN);
        }
        return u;
    }
}
