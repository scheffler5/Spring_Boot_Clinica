package com.learn.projeto_learn.controller.convenio;

import com.learn.projeto_learn.dto.convenio.ConvenioRequestDTO;
import com.learn.projeto_learn.dto.convenio.ConvenioResponseDTO;
import com.learn.projeto_learn.service.convenio.ConvenioService;
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
public class ConvenioController {

    @Autowired
    private ConvenioService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConvenioResponseDTO> create(@RequestBody @Valid ConvenioRequestDTO data,
                                                      UriComponentsBuilder uriBuilder) {
        ConvenioResponseDTO response = service.create(data);
        var uri = uriBuilder.path("/convenios/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ConvenioResponseDTO>> list() {
        return ResponseEntity.ok(service.listActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConvenioResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConvenioResponseDTO> update(@PathVariable UUID id,
                                                      @RequestBody @Valid ConvenioRequestDTO data) {
        return ResponseEntity.ok(service.update(id, data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
