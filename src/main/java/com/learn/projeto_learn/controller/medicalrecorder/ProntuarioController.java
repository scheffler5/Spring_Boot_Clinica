package com.learn.projeto_learn.controller.medicalrecorder;

import com.learn.projeto_learn.dto.medicalrecord.ProntuarioRequestDTO;
import com.learn.projeto_learn.dto.medicalrecord.ProntuarioResponseDTO;
import com.learn.projeto_learn.model.medicalrecord.Prontuario;
import com.learn.projeto_learn.service.medicalrecord.ProntuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/prontuarios")
public class ProntuarioController {

    @Autowired
    private ProntuarioService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDIC')")
    public ResponseEntity<ProntuarioResponseDTO> create(@RequestBody @Valid ProntuarioRequestDTO data,
                                                        UriComponentsBuilder uriBuilder) {
        Prontuario novo = service.create(data);
        var uri = uriBuilder.path("/prontuarios/{id}").buildAndExpand(novo.getId()).toUri();
        return ResponseEntity.created(uri).body(new ProntuarioResponseDTO(novo));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDIC')")
    public ResponseEntity<List<ProntuarioResponseDTO>> list() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDIC')")
    public ResponseEntity<List<ProntuarioResponseDTO>> listByPaciente(@PathVariable UUID pacienteId) {
        return ResponseEntity.ok(service.listByPaciente(pacienteId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDIC')")
    public ResponseEntity<ProntuarioResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
