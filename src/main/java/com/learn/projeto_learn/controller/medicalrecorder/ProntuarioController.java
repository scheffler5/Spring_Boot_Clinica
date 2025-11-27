package com.learn.projeto_learn.controller.medicalrecorder;

import com.learn.projeto_learn.Domain.medicalrecord.Prontuario;
import com.learn.projeto_learn.dto.medicalrecord.ProntuarioRequestDTO;
import com.learn.projeto_learn.service.medicalrecord.ProntuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/prontuarios")
public class ProntuarioController {

    @Autowired
    private ProntuarioService service;

    @PostMapping
    public ResponseEntity<Prontuario> create(@RequestBody @Valid ProntuarioRequestDTO data, UriComponentsBuilder uriBuilder) {
        Prontuario novoProntuario = service.create(data);
        var uri = uriBuilder.path("/prontuarios/{id}").buildAndExpand(novoProntuario.getId()).toUri();
        return ResponseEntity.created(uri).body(novoProntuario);
    }
}