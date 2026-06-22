package com.learn.projeto_learn.controller.Login;

import com.learn.projeto_learn.Infra.Security.IpBlockingService;
import com.learn.projeto_learn.Infra.Security.TokenService;
import com.learn.projeto_learn.dto.Login.*;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.repository.UsuarioRepository;
import com.learn.projeto_learn.service.captcha.CaptchaService;
import com.learn.projeto_learn.service.validation.EmailValidationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired private TokenService tokenService;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UsuarioRepository repository;
    @Autowired private CaptchaService captchaService;
    @Autowired private IpBlockingService ipBlockingService;
    @Autowired private EmailValidationService emailValidationService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data,
                                                  HttpServletRequest request) {
        String ip = resolveIp(request);

        if (ipBlockingService.isBlocked(ip)) {
            throw new BusinessException(
                    "IP bloqueado por excesso de tentativas. Aguarde 15 minutos.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
        if (!captchaService.validate(data.captchaId(), data.captchaCode())) {
            throw new BusinessException("Verificação de segurança inválida. Tente novamente.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(data.login(), data.password()));

            ipBlockingService.registerSuccess(ip);
            Usuario user = (Usuario) repository.findByLogin(data.login());
            return ResponseEntity.ok(new LoginResponseDTO(tokenService.generateToken(user), user.getRole().name()));

        } catch (DisabledException ex) {
            throw new BusinessException("Conta desativada. Entre em contato com o administrador.", HttpStatus.FORBIDDEN);
        } catch (BadCredentialsException ex) {
            ipBlockingService.registerFailure(ip);
            int remaining = ipBlockingService.remainingAttempts(ip);
            String msg = remaining > 0
                    ? "Login ou senha inválidos. Tentativas restantes: " + remaining
                    : "IP bloqueado. Aguarde 15 minutos.";
            throw new BusinessException(msg, HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterDTO data) {
        if (!captchaService.validate(data.captchaId(), data.captchaCode())) {
            throw new BusinessException("Verificação de segurança inválida.");
        }
        emailValidationService.validate(data.email());

        if (repository.findByLogin(data.login()) != null) throw new BusinessException("Login já está em uso.");
        if (repository.existsByEmail(data.email()))        throw new BusinessException("E-mail já cadastrado.");

        String hash = new BCryptPasswordEncoder().encode(data.password());
        repository.save(new Usuario(data.login(), data.email(), hash, data.role()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(repository.findAll().stream().map(UserResponseDTO::new).toList());
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();
        return request.getRemoteAddr();
    }
}
