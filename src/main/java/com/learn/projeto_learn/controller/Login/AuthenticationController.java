package com.learn.projeto_learn.controller.Login;

import com.learn.projeto_learn.Domain.User.Usuario;
import com.learn.projeto_learn.dto.Login.*;
import com.learn.projeto_learn.repository.UsuarioRepository;
import com.learn.projeto_learn.service.Login.AuthorizationService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.learn.projeto_learn.Infra.Security.TokenService;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    AuthorizationService authorizationService;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid AuthenticationDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);
        var token = tokenService.generateToken((Usuario) auth.getPrincipal());
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody @Valid RegisterDTO data) {
        if (this.repository.findByLogin(data.login()) != null) return ResponseEntity.badRequest().build();
        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        Usuario newUser = new Usuario(data.login(), data.email(), encryptedPassword, data.role());
        this.repository.save(newUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        var users = this.repository.findAll().stream().map(UserResponseDTO::new).toList();
        return ResponseEntity.ok(users);
    }
    @PostMapping("/request-recovery")
    public ResponseEntity<String> requestRecovery(@RequestBody @Valid EmailValidationDTO data) {

        boolean emailSent = authorizationService.requestPasswordRecovery(data.email());

        if (emailSent) {
            return ResponseEntity.ok("Código de recuperação enviado para o e-mail.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("E-mail não encontrado no sistema.");
        }
    }
    @PostMapping("/validate-recovery")
    public ResponseEntity<String> validateRecoveryToken(@RequestBody @Valid ValidationCodeDTO data) {

        boolean isValid = authorizationService.validateRecoveryCode(data.email(), data.code());

        if (isValid) {
            return ResponseEntity.ok("Código válido. Pode prosseguir para troca de senha.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Código inválido ou expirado.");
        }
    }
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody @Valid ChangePasswordDTO data) {
        boolean success = authorizationService.changePassword(data.email(), data.code(), data.newPassword());
        if (success) {
            return ResponseEntity.ok("Senha alterada com sucesso!");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Código inválido ou e-mail incorreto.");
        }
    }
}
