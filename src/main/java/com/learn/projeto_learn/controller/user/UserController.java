package com.learn.projeto_learn.controller.user;

import com.learn.projeto_learn.dto.user.MedicoResponseDTO;
import com.learn.projeto_learn.model.User.UserRole;
import com.learn.projeto_learn.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@Tag(name = "Usuários", description = "Consulta de usuários da clínica")
public class UserController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/medicos")
    @Operation(summary = "Lista os médicos ativos")
    public ResponseEntity<List<MedicoResponseDTO>> listMedicos() {
        List<MedicoResponseDTO> medicos = usuarioRepository
                .findAllByRoleInAndAtivoTrue(List.of(UserRole.MEDIC, UserRole.ADMIN))
                .stream()
                .map(MedicoResponseDTO::new)
                .toList();
        return ResponseEntity.ok(medicos);
    }
}
