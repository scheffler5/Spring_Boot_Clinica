package com.learn.projeto_learn.service.patient;

import com.learn.projeto_learn.dto.Login.PatientRegisterDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.User.UserRole;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.model.patient.Paciente;
import com.learn.projeto_learn.repository.PacienteRepository;
import com.learn.projeto_learn.repository.UsuarioRepository;
import com.learn.projeto_learn.service.Login.AuthorizationService;
import com.learn.projeto_learn.service.validation.EmailValidationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PatientAuthService {

    @Autowired private PacienteRepository pacienteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AuthorizationService authorizationService;
    @Autowired private EmailValidationService emailValidationService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Transactional
    public void register(PatientRegisterDTO data) {

        emailValidationService.validate(data.email());

        if (usuarioRepository.existsByEmail(data.email())) {
            throw new BusinessException("Este e-mail já está cadastrado no sistema.");
        }

        pacienteRepository.findByCpf(sanitizeCpf(data.cpf())).ifPresent(existing -> {
            if (usuarioRepository.existsByPaciente(existing)) {
                throw new BusinessException(
                        "Já existe uma conta para este CPF. Use a opção 'Recuperar senha' se esqueceu o acesso.",
                        HttpStatus.CONFLICT);
            }
        });

        String cpfLimpo = sanitizeCpf(data.cpf());
        Paciente paciente = pacienteRepository.findByCpf(cpfLimpo)
                .orElseGet(() -> {
                    Paciente novo = new Paciente(data.nome(), cpfLimpo, data.dataNascimento());
                    return pacienteRepository.save(novo);
                });

        if (usuarioRepository.findByLogin(cpfLimpo) != null) {
            throw new BusinessException("CPF já possui conta no sistema.");
        }

        String hash = passwordEncoder.encode(data.password());
        Usuario usuario = new Usuario(cpfLimpo, data.email(), hash, UserRole.PACIENTE, paciente);
        usuarioRepository.save(usuario);

        authorizationService.sendEmailVerification(data.email());
    }

    private String sanitizeCpf(String cpf) {
        return cpf == null ? null : cpf.replaceAll("[^0-9]", "");
    }
}
