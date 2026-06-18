package com.learn.projeto_learn.service.medicalrecord;

import com.learn.projeto_learn.dto.medicalrecord.ProntuarioRequestDTO;
import com.learn.projeto_learn.dto.medicalrecord.ProntuarioResponseDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.model.insurance.Convenio;
import com.learn.projeto_learn.model.medicalrecord.Prontuario;
import com.learn.projeto_learn.model.patient.Paciente;
import com.learn.projeto_learn.model.procedure.Procedimento;
import com.learn.projeto_learn.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProntuarioService {

    @Autowired private ProntuarioRepository prontuarioRepository;
    @Autowired private PacienteRepository pacienteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ConvenioRepository convenioRepository;
    @Autowired private ProcedimentoRepository procedimentoRepository;

    public Prontuario create(ProntuarioRequestDTO data) {
        Paciente paciente = pacienteRepository.findById(data.idPaciente())
                .filter(p -> Boolean.TRUE.equals(p.getAtivo()))
                .orElseThrow(() -> new BusinessException("Paciente não encontrado.", HttpStatus.NOT_FOUND));

        Usuario medico = usuarioRepository.findById(data.idMedico())
                .orElseThrow(() -> new BusinessException("Médico não encontrado.", HttpStatus.NOT_FOUND));

        Convenio convenio = convenioRepository.findById(data.idConvenio())
                .filter(c -> Boolean.TRUE.equals(c.getAtivo()))
                .orElseThrow(() -> new BusinessException("Convênio não encontrado ou inativo.", HttpStatus.NOT_FOUND));

        Procedimento procedimento = procedimentoRepository.findById(data.idProcedimento())
                .filter(p -> Boolean.TRUE.equals(p.getAtivo()))
                .orElseThrow(() -> new BusinessException("Procedimento não encontrado ou inativo.", HttpStatus.NOT_FOUND));

        BigDecimal custo = procedimento.getCusto();
        BigDecimal valorFinal = custo.subtract(custo.multiply(convenio.getDesconto()));

        Prontuario prontuario = new Prontuario();
        prontuario.setPaciente(paciente);
        prontuario.setMedico(medico);
        prontuario.setConvenio(convenio);
        prontuario.setProcedimento(procedimento);
        prontuario.setObservacoes(data.observacoes());
        prontuario.setValorCalculado(valorFinal);

        return prontuarioRepository.save(prontuario);
    }

    public List<ProntuarioResponseDTO> listAll() {
        return prontuarioRepository.findAll().stream()
                .map(ProntuarioResponseDTO::new)
                .toList();
    }

    public List<ProntuarioResponseDTO> listByPaciente(UUID pacienteId) {
        return prontuarioRepository.findAllByPacienteId(pacienteId).stream()
                .map(ProntuarioResponseDTO::new)
                .toList();
    }

    public ProntuarioResponseDTO findById(UUID id) {
        return prontuarioRepository.findById(id)
                .map(ProntuarioResponseDTO::new)
                .orElseThrow(() -> new BusinessException("Prontuário não encontrado.", HttpStatus.NOT_FOUND));
    }
}
