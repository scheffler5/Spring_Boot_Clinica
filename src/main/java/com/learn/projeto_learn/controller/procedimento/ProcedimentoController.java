package com.learn.projeto_learn.controller.procedimento;

import com.learn.projeto_learn.dto.procedimento.ProcedimentoRequestDTO;
import com.learn.projeto_learn.dto.procedimento.ProcedimentoResponseDTO;
import com.learn.projeto_learn.service.procedimento.ProcedimentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/procedimentos")
public class ProcedimentoController {

    @Autowired
    private ProcedimentoService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProcedimentoResponseDTO> create(@RequestBody @Valid ProcedimentoRequestDTO data,
                                                          UriComponentsBuilder uriBuilder) {
        ProcedimentoResponseDTO response = service.create(data);
        var uri = uriBuilder.path("/procedimentos/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProcedimentoResponseDTO>> list() {
        return ResponseEntity.ok(service.listActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcedimentoResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProcedimentoResponseDTO> update(@PathVariable UUID id,
                                                          @RequestBody @Valid ProcedimentoRequestDTO data) {
        return ResponseEntity.ok(service.update(id, data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
