package com.learn.projeto_learn.controller.convenio;

import com.learn.projeto_learn.dto.convenio.ConvenioRequestDTO;
import com.learn.projeto_learn.dto.convenio.ConvenioResponseDTO;
import com.learn.projeto_learn.service.convenio.ConvenioService;
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
@RequestMapping("/convenios")
@Tag(name = "Convênios", description = "Cadastro de convênios médicos")
public class ConvenioController {

    @Autowired
    private ConvenioService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cria um convênio (ADMIN)")
    public ResponseEntity<ConvenioResponseDTO> create(@RequestBody @Valid ConvenioRequestDTO data,
                                                      UriComponentsBuilder uriBuilder) {
        ConvenioResponseDTO response = service.create(data);
        var uri = uriBuilder.path("/convenios/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    @Operation(summary = "Lista os convênios ativos")
    public ResponseEntity<List<ConvenioResponseDTO>> list() {
        return ResponseEntity.ok(service.listActive());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um convênio por ID")
    public ResponseEntity<ConvenioResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza um convênio (ADMIN)")
    public ResponseEntity<ConvenioResponseDTO> update(@PathVariable UUID id,
                                                      @RequestBody @Valid ConvenioRequestDTO data) {
        return ResponseEntity.ok(service.update(id, data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativa um convênio (ADMIN)")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
