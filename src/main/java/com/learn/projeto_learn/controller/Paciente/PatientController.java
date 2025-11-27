package com.learn.projeto_learn.controller.Paciente;

import com.learn.projeto_learn.Domain.patient.Paciente;
import com.learn.projeto_learn.dto.paciente.PatientRequestDTO;
import com.learn.projeto_learn.dto.paciente.PatientResponseDTO;
import com.learn.projeto_learn.service.Paciente.PatientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/patients")
public class PatientController {
    @Autowired
    private PatientService service;

    @PostMapping
    public ResponseEntity<Paciente> create(@RequestBody @Valid PatientRequestDTO data, UriComponentsBuilder uriBuilder) {
        Paciente newPatient = service.createPatient(data);
        var uri = uriBuilder.path("/patients/{id}").buildAndExpand(newPatient.getId()).toUri();

        return ResponseEntity.created(uri).body(newPatient);
    }
    @GetMapping
    public ResponseEntity<List<PatientResponseDTO>> list(@RequestParam(required = false) String search) {
        var list = service.listPatients(search);
        return ResponseEntity.ok(list);
    }
}