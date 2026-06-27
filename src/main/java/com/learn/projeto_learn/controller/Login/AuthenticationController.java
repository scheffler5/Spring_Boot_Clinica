package com.learn.projeto_learn.controller.Login;

import com.learn.projeto_learn.Infra.Security.IpBlockingService;
import com.learn.projeto_learn.Infra.Security.TokenService;
import com.learn.projeto_learn.dto.Login.*;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.User.UserRole;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.repository.UsuarioRepository;
import com.learn.projeto_learn.service.captcha.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Login e registro de médicos")
public class AuthenticationController {

    @Autowired private TokenService          tokenService;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UsuarioRepository     repository;
    @Autowired private CaptchaService        captchaService;
    @Autowired private IpBlockingService     ipBlockingService;
    @Autowired private PasswordEncoder       passwordEncoder;

    @PostMapping("/login")
    @Operation(summary = "Autentica e retorna um token JWT",
            description = "Valida CAPTCHA e credenciais. Bloqueia o IP após múltiplas tentativas. Endpoint público.",
            security = {})
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data,
                                                  HttpServletRequest request) {
        String ip = resolveIp(request);

        if (ipBlockingService.isBlocked(ip)) {
            throw new BusinessException("IP bloqueado. Aguarde 15 minutos.", HttpStatus.TOO_MANY_REQUESTS);
        }
        if (!captchaService.validate(data.captchaId(), data.captchaCode())) {
            throw new BusinessException("Verificação de segurança inválida. Tente novamente.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(data.login(), data.password()));

            ipBlockingService.registerSuccess(ip);
            Usuario user = (Usuario) repository.findByLogin(data.login());

            return ResponseEntity.ok(new LoginResponseDTO(
                    tokenService.generateToken(user),
                    user.getRole().name(),
                    Boolean.TRUE.equals(user.getPerfilCompleto())
            ));

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
    @Operation(summary = "Registra um novo médico",
            description = "Cria um usuário com papel MEDIC após validar o CAPTCHA. Endpoint público.",
            security = {})
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterDTO data) {
        if (!captchaService.validate(data.captchaId(), data.captchaCode())) {
            throw new BusinessException("Verificação de segurança inválida.");
        }
        if (repository.findByLogin(data.login()) != null) {
            throw new BusinessException("Login já está em uso.");
        }

        String hash = passwordEncoder.encode(data.password());
        repository.save(new Usuario(data.login(), hash, UserRole.MEDIC));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();
        return request.getRemoteAddr();
    }
}
