package com.learn.projeto_learn.service.patient;

import com.learn.projeto_learn.dto.login.PatientRegisterDTO;
import com.learn.projeto_learn.dto.patient.CompleteProfileDTO;
import com.learn.projeto_learn.dto.patient.PatientResponseDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.user.UserRole;
import com.learn.projeto_learn.model.user.Usuario;
import com.learn.projeto_learn.model.patient.Paciente;
import com.learn.projeto_learn.repository.PacienteRepository;
import com.learn.projeto_learn.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PatientAuthService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PacienteRepository pacienteRepository;
    @Autowired private PasswordEncoder   passwordEncoder;

    @Transactional
    public void register(PatientRegisterDTO data) {
        if (usuarioRepository.findByLogin(data.login()) != null) {
            throw new BusinessException("Login já está em uso.");
        }

        String hash = passwordEncoder.encode(data.password());
        usuarioRepository.save(new Usuario(data.login(), hash, UserRole.PACIENTE));
    }

    @Transactional
    public PatientResponseDTO completeProfile(Usuario user, CompleteProfileDTO data) {
        if (Boolean.TRUE.equals(user.getPerfilCompleto()) && user.getPaciente() != null) {
            throw new BusinessException("Seu perfil já foi completado.", HttpStatus.CONFLICT);
        }

        String cpfLimpo = sanitizeCpf(data.cpf());

        Paciente paciente = pacienteRepository.findByCpf(cpfLimpo)
                .map(existing -> {
                    usuarioRepository.findByPaciente(existing).ifPresent(dono -> {
                        if (!dono.getId().equals(user.getId())) {
                            throw new BusinessException(
                                    "Este CPF já está vinculado a outra conta. Use 'Recuperar senha' se for você.",
                                    HttpStatus.CONFLICT);
                        }
                    });
                    boolean nomeBate = existing.getNome().trim().equalsIgnoreCase(data.nome().trim());
                    boolean nascimentoBate = existing.getDataNascimento().equals(data.dataNascimento());
                    if (!nomeBate || !nascimentoBate) {
                        throw new BusinessException(
                                "Os dados informados não correspondem ao cadastro existente para este CPF.",
                                HttpStatus.UNPROCESSABLE_ENTITY);
                    }
                    existing.setNomeMae(data.nomeMae());
                    return existing;
                })
                .orElseGet(() -> {
                    Paciente novo = new Paciente(data.nome(), cpfLimpo, data.dataNascimento());
                    novo.setNomeMae(data.nomeMae());
                    return pacienteRepository.save(novo);
                });

        user.setPaciente(paciente);
        user.setNome(data.nome());
        user.setPerfilCompleto(true);
        usuarioRepository.save(user);

        return new PatientResponseDTO(paciente);
    }

    private String sanitizeCpf(String cpf) {
        return cpf == null ? null : cpf.replaceAll("[^0-9]", "");
    }
}
