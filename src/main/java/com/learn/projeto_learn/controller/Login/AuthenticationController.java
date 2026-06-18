package com.learn.projeto_learn.controller.Login;

import com.learn.projeto_learn.Infra.Security.IpBlockingService;
import com.learn.projeto_learn.Infra.Security.MfaAttemptService;
import com.learn.projeto_learn.Infra.Security.TokenService;
import com.learn.projeto_learn.dto.Login.*;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.repository.UsuarioRepository;
import com.learn.projeto_learn.service.Login.AuthorizationService;
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
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired private TokenService tokenService;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UsuarioRepository repository;
    @Autowired private AuthorizationService authorizationService;
    @Autowired private CaptchaService captchaService;
    @Autowired private IpBlockingService ipBlockingService;
    @Autowired private MfaAttemptService mfaAttemptService;
    @Autowired private EmailValidationService emailValidationService;


    @PostMapping("/login")
    public ResponseEntity<MfaRequiredResponseDTO> login(@RequestBody @Valid AuthenticationDTO data,
                                                        HttpServletRequest request) {
        String ip = resolveIp(request);

        if (ipBlockingService.isBlocked(ip)) {
            throw new BusinessException(
                    "IP bloqueado por excesso de tentativas. Aguarde 15 minutos.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
        if (!captchaService.validate(data.captchaId(), data.captchaCode())) {
            throw new BusinessException("CAPTCHA inválido ou expirado. Atualize a página e tente novamente.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(data.login(), data.password()));

            String email = authorizationService.sendMfaCode(data.login());
            ipBlockingService.registerSuccess(ip);
            mfaAttemptService.registerSuccess(email);

            return ResponseEntity.ok(new MfaRequiredResponseDTO(true, maskEmail(email), email));

        } catch (DisabledException ex) {
            throw new BusinessException(
                    "Conta não ativada. Verifique seu e-mail antes de fazer login.",
                    HttpStatus.FORBIDDEN);
        } catch (BadCredentialsException ex) {
            ipBlockingService.registerFailure(ip);
            int remaining = ipBlockingService.remainingAttempts(ip);
            String msg = remaining > 0
                    ? "Login ou senha inválidos. Tentativas restantes: " + remaining
                    : "IP bloqueado. Aguarde 15 minutos.";
            throw new BusinessException(msg, HttpStatus.UNAUTHORIZED);
        }
    }


    @PostMapping("/verify-mfa")
    public ResponseEntity<LoginResponseDTO> verifyMfa(@RequestBody @Valid MfaVerifyDTO data,
                                                      HttpServletRequest request) {
        String email = data.email();

        if (mfaAttemptService.isExhausted(email)) {
            throw new BusinessException(
                    "Tentativas esgotadas. Solicite um novo código ou faça login novamente.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        String login = authorizationService.verifyMfaAndGetLogin(email, data.mfaCode());

        if (login == null) {
            int remaining = mfaAttemptService.registerFailure(email);

            if (remaining <= 0) {

                authorizationService.invalidateMfaToken(email);
                throw new BusinessException(
                        "Código incorreto 3 vezes. Use o botão 'Reenviar código' para receber um novo.",
                        HttpStatus.TOO_MANY_REQUESTS);
            }

            throw new BusinessException(
                    "Código MFA inválido. " + remaining + " tentativa(s) restante(s).",
                    HttpStatus.UNAUTHORIZED);
        }

        mfaAttemptService.registerSuccess(email);
        Usuario user = (Usuario) repository.findByLogin(login);
        return ResponseEntity.ok(new LoginResponseDTO(tokenService.generateToken(user), user.getRole().name()));
    }


    @PostMapping("/resend-mfa")
    public ResponseEntity<?> resendMfa(@RequestBody @Valid MfaResendDTO data) {
        String email = data.email();

        if (!mfaAttemptService.canResend(email)) {
            int cooldown = mfaAttemptService.resendCooldownSecondsRemaining(email);
            int resends  = mfaAttemptService.remainingResends(email);

            if (resends <= 0) {
                throw new BusinessException(
                        "Limite de reenvios atingido. Faça login novamente para obter um novo código.",
                        HttpStatus.TOO_MANY_REQUESTS);
            }
            throw new BusinessException(
                    "Aguarde " + cooldown + " segundo(s) antes de solicitar um novo código.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        String sentTo = authorizationService.resendMfaCode(email);
        if (sentTo == null) {
            throw new BusinessException("E-mail não encontrado.", HttpStatus.NOT_FOUND);
        }

        mfaAttemptService.registerResend(email);
        int remaining = mfaAttemptService.remainingResends(email);

        return ResponseEntity.ok(Map.of(
                "emailHint",       maskEmail(sentTo),
                "remainingResends", remaining,
                "cooldownSeconds",  60
        ));
    }


    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterDTO data) {
        if (!captchaService.validate(data.captchaId(), data.captchaCode())) {
            throw new BusinessException("CAPTCHA inválido ou expirado.");
        }
        emailValidationService.validate(data.email());
        if (repository.findByLogin(data.login()) != null) throw new BusinessException("Login já está em uso.");
        if (repository.existsByEmail(data.email()))        throw new BusinessException("E-mail já cadastrado.");

        String hash = new BCryptPasswordEncoder().encode(data.password());
        repository.save(new Usuario(data.login(), data.email(), hash, data.role()));
        authorizationService.sendEmailVerification(data.email());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestBody @Valid EmailVerifyDTO data) {
        authorizationService.verifyEmail(data.email(), data.code());
        return ResponseEntity.ok("E-mail verificado com sucesso. Agora você pode fazer login.");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(@RequestBody @Valid EmailValidationDTO data) {
        authorizationService.sendEmailVerification(data.email());
        return ResponseEntity.ok("Se o e-mail estiver cadastrado e pendente de verificação, um novo código será enviado.");
    }


    @PostMapping("/request-recovery")
    public ResponseEntity<String> requestRecovery(@RequestBody @Valid EmailValidationDTO data) {
        authorizationService.requestPasswordRecovery(data.email());
        return ResponseEntity.ok("Se o e-mail estiver cadastrado, um código de recuperação será enviado.");
    }

    @PostMapping("/validate-recovery")
    public ResponseEntity<String> validateRecovery(@RequestBody @Valid ValidationCodeDTO data) {
        boolean valid = authorizationService.validateRecoveryCode(data.email(), data.code());
        if (!valid) throw new BusinessException("Código inválido ou expirado.", HttpStatus.BAD_REQUEST);
        return ResponseEntity.ok("Código válido. Prossiga para a troca de senha.");
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody @Valid ChangePasswordDTO data) {
        boolean ok = authorizationService.changePassword(data.email(), data.code(), data.newPassword());
        if (!ok) throw new BusinessException("Código inválido ou e-mail incorreto.", HttpStatus.BAD_REQUEST);
        return ResponseEntity.ok("Senha alterada com sucesso.");
    }


    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(repository.findAll().stream().map(UserResponseDTO::new).toList());
    }


    private String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 2) return "***" + email.substring(at);
        return email.substring(0, 2) + "***" + email.substring(at);
    }

    private String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();
        return request.getRemoteAddr();
    }
}
