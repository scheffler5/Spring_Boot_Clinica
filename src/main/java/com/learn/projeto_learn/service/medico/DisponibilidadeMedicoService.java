package com.learn.projeto_learn.service.medico;

import com.learn.projeto_learn.dto.medico.DisponibilidadeRequestDTO;
import com.learn.projeto_learn.dto.medico.DisponibilidadeResponseDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.agendamento.DisponibilidadeMedico;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.repository.DisponibilidadeMedicoRepository;
import com.learn.projeto_learn.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DisponibilidadeMedicoService {

    @Autowired private DisponibilidadeMedicoRepository disponibilidadeRepository;
    @Autowired private UsuarioRepository               usuarioRepository;

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
}
