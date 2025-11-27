package com.learn.projeto_learn.service.Paciente;

import com.learn.projeto_learn.Domain.patient.Paciente;
import com.learn.projeto_learn.dto.paciente.PatientRequestDTO;
import com.learn.projeto_learn.dto.paciente.PatientResponseDTO;
import com.learn.projeto_learn.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientService {
    @Autowired
    private PacienteRepository repository;

    public Paciente createPatient(PatientRequestDTO data) {
        if (repository.existsByCpf(data.cpf())) {
            throw new RuntimeException("Já existe um paciente cadastrado com este CPF.");
        }
        Paciente newPatient = new Paciente(data.nome(), data.cpf(), data.dataNascimento());

        if (isMinor(data.dataNascimento())) {
            if (data.nomeMae() == null || data.nomeMae().isBlank() ||
                    data.nomePai() == null || data.nomePai().isBlank()) {
                throw new RuntimeException("Para menores de 18 anos, o nome do pai e da mãe são obrigatórios.");
            }
            newPatient.setNomeMae(data.nomeMae());
            newPatient.setNomePai(data.nomePai());
        } else {
            newPatient.setNomeMae(null);
            newPatient.setNomePai(null);
        }

        return repository.save(newPatient);
    }

    // Método auxiliar para calcular idade
    private boolean isMinor(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears() < 18;
    }
    public List<PatientResponseDTO> listPatients(String search) {
        List<Paciente> pacientes;
        if (search != null && !search.isBlank()) {
            pacientes = repository.findByNomeContainingIgnoreCaseOrCpf(search, search);
        } else {
            pacientes = repository.findAll();
        }
        return pacientes.stream()
                .map(PatientResponseDTO::new)
                .collect(Collectors.toList());
    }
}