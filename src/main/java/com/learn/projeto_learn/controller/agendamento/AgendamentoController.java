package com.learn.projeto_learn.controller.agendamento;

import com.learn.projeto_learn.dto.agendamento.AppointmentRequestDTO;
import com.learn.projeto_learn.dto.agendamento.AppointmentResponseDTO;
import com.learn.projeto_learn.dto.agendamento.AppointmentStatusDTO;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.model.agendamento.Agendamento;
import com.learn.projeto_learn.service.Agendamento.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONIST', 'MEDIC')")
public class AgendamentoController {

    @Autowired private AgendamentoService service;

    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> create(@RequestBody @Valid AppointmentRequestDTO data,
                                                         UriComponentsBuilder uriBuilder) {
        Agendamento appointment = service.createAppointment(data);
        var uri = uriBuilder.path("/appointments/{id}").buildAndExpand(appointment.getId()).toUri();
        return ResponseEntity.created(uri).body(new AppointmentResponseDTO(appointment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> cancelar(@PathVariable UUID id,
                                                           @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.cancelar(id, usuario));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> list() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<AppointmentResponseDTO>> listByPaciente(@PathVariable UUID pacienteId) {
        return ResponseEntity.ok(service.listByPaciente(pacienteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentResponseDTO> updateStatus(@PathVariable UUID id,
                                                               @RequestBody @Valid AppointmentStatusDTO data) {
        return ResponseEntity.ok(service.updateStatus(id, data.status()));
    }
}
