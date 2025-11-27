package com.learn.projeto_learn.service.medicalrecord;

import com.learn.projeto_learn.Domain.User.Usuario;
import com.learn.projeto_learn.Domain.insurance.Convenio;
import com.learn.projeto_learn.Domain.medicalrecord.Prontuario;
import com.learn.projeto_learn.Domain.patient.Paciente;
import com.learn.projeto_learn.Domain.procedure.Procedimento;
import com.learn.projeto_learn.dto.medicalrecord.ProntuarioRequestDTO;
import com.learn.projeto_learn.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ProntuarioService {

    @Autowired private ProntuarioRepository prontuarioRepository;
    @Autowired private PacienteRepository pacienteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ConvenioRepository convenioRepository;
    @Autowired private ProcedimentoRepository procedimentoRepository;

    public Prontuario create(ProntuarioRequestDTO data) {
        Paciente paciente = pacienteRepository.findById(data.idPaciente())
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));

        Usuario medico = usuarioRepository.findById(data.idMedico())
                .orElseThrow(() -> new RuntimeException("Médico não encontrado"));

        Convenio convenio = convenioRepository.findById(data.idConvenio())
                .orElseThrow(() -> new RuntimeException("Convênio não encontrado"));

        Procedimento procedimento = procedimentoRepository.findById(data.idProcedimento())
                .orElseThrow(() -> new RuntimeException("Procedimento não encontrado"));
        BigDecimal custoBase = procedimento.getCusto();
        BigDecimal desconto = convenio.getDesconto();
        BigDecimal valorDoDesconto = custoBase.multiply(desconto);
        BigDecimal valorFinal = custoBase.subtract(valorDoDesconto);
        Prontuario prontuario = new Prontuario();
        prontuario.setPaciente(paciente);
        prontuario.setMedico(medico);
        prontuario.setConvenio(convenio);
        prontuario.setProcedimento(procedimento);
        prontuario.setObservacoes(data.observacoes());

        prontuario.setValorCalculado(valorFinal);

        return prontuarioRepository.save(prontuario);
    }
}