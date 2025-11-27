package com.learn.projeto_learn.service.Agendamento;

import com.learn.projeto_learn.Domain.agendamento.Agendamento;
import com.learn.projeto_learn.Domain.patient.Paciente;
import com.learn.projeto_learn.dto.agendamento.AppointmentRequestDTO;
import com.learn.projeto_learn.repository.AgendamentoRepository;
import com.learn.projeto_learn.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    public Agendamento createAppointment(AppointmentRequestDTO data) {
        // 1. Busca o Paciente (Garante que ele existe)
        Paciente paciente = pacienteRepository.findById(data.pacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente não encontrado com o ID fornecido."));

        // 2. Valida Conflito de Horário (Simples)
        // Nota: Num sistema real com vários médicos, verificaríamos (Data + MedicoID)
        if (agendamentoRepository.existsByDataHora(data.dataHora())) {
            throw new RuntimeException("Já existe um agendamento para este horário.");
        }

        // 3. Salva
        Agendamento novoAgendamento = new Agendamento(paciente, data.dataHora());
        return agendamentoRepository.save(novoAgendamento);
    }
}