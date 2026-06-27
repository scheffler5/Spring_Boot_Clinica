package com.learn.projeto_learn.controller.agendamento;

import com.learn.projeto_learn.dto.agendamento.AppointmentResponseDTO;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.service.Agendamento.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@Tag(name = "Agendamentos", description = "Cancelamento de consultas")
public class AgendamentoController {

    @Autowired private AgendamentoService service;

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDIC', 'PACIENTE')")
    @Operation(summary = "Cancela um agendamento",
            description = "O médico cancela suas consultas; o paciente cancela a própria " +
                    "(regras de antecedência e de posse são validadas no serviço).")
    public ResponseEntity<AppointmentResponseDTO> cancelar(@PathVariable UUID id,
                                                           @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.cancelar(id, usuario));
    }
}
