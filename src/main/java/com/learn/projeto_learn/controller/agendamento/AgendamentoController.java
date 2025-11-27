package com.learn.projeto_learn.controller.agendamento;

import com.learn.projeto_learn.Domain.agendamento.Agendamento;
import com.learn.projeto_learn.dto.agendamento.AppointmentRequestDTO;
import com.learn.projeto_learn.service.Agendamento.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/appointments")
public class AgendamentoController {

    @Autowired
    private AgendamentoService service;

    @PostMapping
    public ResponseEntity<Agendamento> create(@RequestBody @Valid AppointmentRequestDTO data, UriComponentsBuilder uriBuilder) {
        Agendamento appointment = service.createAppointment(data);

        var uri = uriBuilder.path("/appointments/{id}").buildAndExpand(appointment.getId()).toUri();

        return ResponseEntity.created(uri).body(appointment);
    }
}