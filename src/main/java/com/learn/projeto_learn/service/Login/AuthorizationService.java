package com.learn.projeto_learn.service.Login; // Confirme se o pacote está certo

import com.learn.projeto_learn.Domain.User.Usuario;
import com.learn.projeto_learn.repository.UsuarioRepository; // Import do seu Repository
import com.learn.projeto_learn.service.EmailService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthorizationService implements UserDetailsService {

    @Autowired
    UsuarioRepository repository;

    @Autowired
    EmailService emailService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = repository.findByLogin(username);
        if (user == null) {
            throw new UsernameNotFoundException("Usuário não encontrado: " + username);
        }

        return user;
    }
    public void sendRecoveryCode(String email) {
        Usuario user = repository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }
        String code = String.format("%06d", new Random().nextInt(999999));
        user.setTwoFactorToken(code);
        user.setRecoveryTokenExpiration(LocalDateTime.now().plusMinutes(15));
        repository.save(user);
        emailService.sendEmailText(
                user.getEmail(),
                "Recuperação de Senha - Health SaaS",
                "Olá! Seu código de recuperação é: " + code + "\nEste código expira em 15 minutos."
        );
    }

    @Transactional
    public boolean requestPasswordRecovery(String email) { // Mudou de void para boolean
        Usuario user = repository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }
        String code = String.format("%06d", new Random().nextInt(999999));
        user.setTwoFactorToken(code);
        user.setRecoveryTokenExpiration(LocalDateTime.now().plusMinutes(15));
        repository.save(user);
        String corpoEmail = "Olá, " + user.getLogin() + ".\n" +
                "Seu código de recuperação é: " + code + "\n" +
                "Este código é válido até: " + user.getRecoveryTokenExpiration();
        emailService.sendEmailText(user.getEmail(), "Recuperação de Senha", corpoEmail);
        return true;
    }
    public boolean validateRecoveryCode(String email, String codeToCheck) {
        Usuario user = repository.findByEmail(email).orElse(null);
        if (user == null || user.getTwoFactorToken() == null || user.getRecoveryTokenExpiration() == null) {
            return false;
        }
        boolean isCodeCorrect = user.getTwoFactorToken().equals(codeToCheck);
        boolean isExpired = LocalDateTime.now().isAfter(user.getRecoveryTokenExpiration());
        if (isCodeCorrect && !isExpired) {
            return true;
        }
        return false;
    }
    @Transactional
    public boolean changePassword(String email, String code, String newPassword) {
        Usuario user = repository.findByEmail(email).orElse(null);
        if (user == null || user.getTwoFactorToken() == null) {
            return false;
        }
        if (!user.getTwoFactorToken().equals(code)) {
            return false;
        }
        String encryptedPassword = new BCryptPasswordEncoder().encode(newPassword);
        user.setPassword(encryptedPassword);
        user.setTwoFactorToken(null);
        user.setRecoveryTokenExpiration(null);
        repository.save(user);

        return true;
    }
}