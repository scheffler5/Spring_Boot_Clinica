package com.learn.projeto_learn.service.patient;

import com.learn.projeto_learn.dto.Login.PatientRegisterDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.User.UserRole;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PatientAuthService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder   passwordEncoder;

    @Transactional
    public void register(PatientRegisterDTO data) {
        if (usuarioRepository.findByLogin(data.login()) != null) {
            throw new BusinessException("Login já está em uso.");
        }

        String hash = passwordEncoder.encode(data.password());
        usuarioRepository.save(new Usuario(data.login(), hash, UserRole.PACIENTE));
    }
}
