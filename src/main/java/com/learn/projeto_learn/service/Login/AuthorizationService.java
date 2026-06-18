package com.learn.projeto_learn.service.Login;

import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.repository.UsuarioRepository;
import com.learn.projeto_learn.service.EmailService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthorizationService implements UserDetailsService {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private EmailService emailService;

    private final SecureRandom rng = new SecureRandom();

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = repository.findByLogin(username);
        if (user == null) throw new UsernameNotFoundException("Usuário não encontrado: " + username);
        return user;
    }



    @Transactional
    public void sendEmailVerification(String email) {
        Usuario user = repository.findByEmail(email).orElse(null);
        if (user == null || Boolean.TRUE.equals(user.getEmailVerified())) return;

        String code = generateCode();
        user.setEmailVerificationToken(code);
        user.setEmailVerificationExpiration(LocalDateTime.now().plusMinutes(30));
        repository.save(user);

        emailService.sendEmailText(
                email,
                "Verificação de E-mail - Clínica",
                "Olá, " + user.getLogin() + "!\n\n" +
                "Seu código de verificação de e-mail é: " + code + "\n\n" +
                "Este código expira em 30 minutos.\n" +
                "Se você não criou esta conta, ignore este e-mail."
        );
    }

    @Transactional
    public void verifyEmail(String email, String code) {
        Usuario user = repository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("E-mail não encontrado.", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException("E-mail já verificado.");
        }
        if (user.getEmailVerificationToken() == null || user.getEmailVerificationExpiration() == null) {
            throw new BusinessException("Nenhum código de verificação pendente. Solicite o reenvio.");
        }
        if (LocalDateTime.now().isAfter(user.getEmailVerificationExpiration())) {
            throw new BusinessException("Código expirado. Solicite um novo código.", HttpStatus.GONE);
        }
        if (!user.getEmailVerificationToken().equals(code)) {
            throw new BusinessException("Código de verificação inválido.");
        }

        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiration(null);
        user.setEmailVerified(true);
        user.setAtivo(true);
        repository.save(user);
    }



    @Transactional
    public String sendMfaCode(String login) {
        Usuario user = (Usuario) repository.findByLogin(login);
        if (user == null) return null;
        return dispatchMfaCode(user);
    }

    @Transactional
    public String resendMfaCode(String email) {
        Usuario user = repository.findByEmail(email).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getAtivo())) return null;
        return dispatchMfaCode(user);
    }

    @Transactional
    public void invalidateMfaToken(String email) {
        repository.findByEmail(email).ifPresent(user -> {
            user.setMfaToken(null);
            user.setMfaTokenExpiration(null);
            repository.save(user);
        });
    }

    private String dispatchMfaCode(Usuario user) {
        String code = generateCode();
        user.setMfaToken(code);
        user.setMfaTokenExpiration(LocalDateTime.now().plusMinutes(10));
        repository.save(user);

        emailService.sendEmailText(
                user.getEmail(),
                "Código de Acesso (MFA) - Clínica",
                "Olá, " + user.getLogin() + "!\n\n" +
                "Seu código de acesso para login é: " + code + "\n\n" +
                "Válido por 10 minutos. Você tem 3 tentativas.\n" +
                "Se não foi você, troque sua senha imediatamente."
        );
        return user.getEmail();
    }

    @Transactional
    public String verifyMfaAndGetLogin(String email, String code) {
        Usuario user = repository.findByEmail(email).orElse(null);
        if (user == null || user.getMfaToken() == null) return null;
        if (LocalDateTime.now().isAfter(user.getMfaTokenExpiration())) return null;
        if (!user.getMfaToken().equals(code)) return null;

        user.setMfaToken(null);
        user.setMfaTokenExpiration(null);
        repository.save(user);

        return user.getLogin();
    }



    @Transactional
    public boolean requestPasswordRecovery(String email) {
        Usuario user = repository.findByEmail(email).orElse(null);
        if (user == null) return false;

        String code = generateCode();
        user.setTwoFactorToken(code);
        user.setRecoveryTokenExpiration(LocalDateTime.now().plusMinutes(15));
        repository.save(user);

        emailService.sendEmailText(
                user.getEmail(),
                "Recuperação de Senha - Clínica",
                "Olá, " + user.getLogin() + "!\n\n" +
                "Seu código de recuperação é: " + code + "\n" +
                "Válido até: " + user.getRecoveryTokenExpiration() + "\n\n" +
                "Se não foi você, ignore este e-mail."
        );
        return true;
    }

    public boolean validateRecoveryCode(String email, String code) {
        Usuario user = repository.findByEmail(email).orElse(null);
        if (user == null || user.getTwoFactorToken() == null || user.getRecoveryTokenExpiration() == null) {
            return false;
        }
        return user.getTwoFactorToken().equals(code)
                && LocalDateTime.now().isBefore(user.getRecoveryTokenExpiration());
    }

    @Transactional
    public boolean changePassword(String email, String code, String newPassword) {
        Usuario user = repository.findByEmail(email).orElse(null);
        if (user == null || user.getTwoFactorToken() == null) return false;
        if (!user.getTwoFactorToken().equals(code)) return false;

        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        user.setTwoFactorToken(null);
        user.setRecoveryTokenExpiration(null);
        repository.save(user);
        return true;
    }


    private String generateCode() {
        return String.format("%06d", rng.nextInt(1_000_000));
    }
}
