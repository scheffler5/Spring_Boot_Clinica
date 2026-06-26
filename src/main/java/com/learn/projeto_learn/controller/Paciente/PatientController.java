package com.learn.projeto_learn.controller.Paciente;

import com.learn.projeto_learn.dto.paciente.PatientRequestDTO;
import com.learn.projeto_learn.dto.paciente.PatientResponseDTO;
import com.learn.projeto_learn.service.Paciente.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@Tag(name = "Pacientes (gestão)", description = "CRUD de pacientes pela clínica")
public class PatientController {

    @Autowired
    private PatientService service;

    @PostMapping
    @Operation(summary = "Cadastra um paciente")
    public ResponseEntity<PatientResponseDTO> create(@RequestBody @Valid PatientRequestDTO data,
                                                     UriComponentsBuilder uriBuilder) {
        PatientResponseDTO response = service.createPatient(data);
        var uri = uriBuilder.path("/patients/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    @Operation(summary = "Lista pacientes", description = "Filtro opcional por nome/termo de busca.")
    public ResponseEntity<List<PatientResponseDTO>> list(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.listPatients(search));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um paciente por ID")
    public ResponseEntity<PatientResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um paciente")
    public ResponseEntity<PatientResponseDTO> update(@PathVariable UUID id,
                                                     @RequestBody @Valid PatientRequestDTO data) {
        return ResponseEntity.ok(service.updatePatient(id, data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativa um paciente (ADMIN)")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivatePatient(id);
        return ResponseEntity.noContent().build();
    }
}
