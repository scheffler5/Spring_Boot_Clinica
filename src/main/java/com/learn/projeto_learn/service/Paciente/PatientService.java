package com.learn.projeto_learn.service.Paciente;

import com.learn.projeto_learn.dto.paciente.PatientRequestDTO;
import com.learn.projeto_learn.dto.paciente.PatientResponseDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.patient.Paciente;
import com.learn.projeto_learn.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

    @Autowired
    private PacienteRepository repository;

    public PatientResponseDTO createPatient(PatientRequestDTO data) {
        if (repository.existsByCpf(data.cpf())) {
            throw new BusinessException("Já existe um paciente cadastrado com este CPF.");
        }
        Paciente patient = new Paciente(data.nome(), data.cpf(), data.dataNascimento());

        if (isMinor(data.dataNascimento())) {
            if (data.nomeMae() == null || data.nomeMae().isBlank()
                    || data.nomePai() == null || data.nomePai().isBlank()) {
                throw new BusinessException("Para menores de 18 anos, nome do pai e da mãe são obrigatórios.");
            }
            patient.setNomeMae(data.nomeMae());
            patient.setNomePai(data.nomePai());
        }

        return new PatientResponseDTO(repository.save(patient));
    }

    public List<PatientResponseDTO> listPatients(String search) {
        List<Paciente> result = (search != null && !search.isBlank())
                ? repository.findByNomeContainingIgnoreCaseOrCpfAndAtivo(search, search, true)
                : repository.findAllByAtivo(true);
        return result.stream().map(PatientResponseDTO::new).toList();
    }

    public PatientResponseDTO findById(UUID id) {
        return repository.findById(id)
                .filter(p -> Boolean.TRUE.equals(p.getAtivo()))
                .map(PatientResponseDTO::new)
                .orElseThrow(() -> new BusinessException("Paciente não encontrado.", HttpStatus.NOT_FOUND));
    }

    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO data) {
        Paciente patient = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Paciente não encontrado.", HttpStatus.NOT_FOUND));

        if (!patient.getCpf().equals(data.cpf()) && repository.existsByCpf(data.cpf())) {
            throw new BusinessException("CPF já está em uso por outro paciente.");
        }

        patient.setNome(data.nome());
        patient.setCpf(data.cpf());
        patient.setDataNascimento(data.dataNascimento());

        if (isMinor(data.dataNascimento())) {
            if (data.nomeMae() == null || data.nomeMae().isBlank()
                    || data.nomePai() == null || data.nomePai().isBlank()) {
                throw new BusinessException("Para menores de 18 anos, nome do pai e da mãe são obrigatórios.");
            }
            patient.setNomeMae(data.nomeMae());
            patient.setNomePai(data.nomePai());
        } else {
            patient.setNomeMae(null);
            patient.setNomePai(null);
        }

        return new PatientResponseDTO(repository.save(patient));
    }

    public void deactivatePatient(UUID id) {
        Paciente patient = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Paciente não encontrado.", HttpStatus.NOT_FOUND));
        patient.setAtivo(false);
        repository.save(patient);
    }

    private boolean isMinor(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears() < 18;
    }
}
