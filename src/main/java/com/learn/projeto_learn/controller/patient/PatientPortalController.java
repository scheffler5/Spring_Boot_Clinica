package com.learn.projeto_learn.controller.patient;

import com.learn.projeto_learn.dto.Login.PatientRegisterDTO;
import com.learn.projeto_learn.dto.agendamento.AppointmentResponseDTO;
import com.learn.projeto_learn.dto.medicalrecord.ProntuarioResponseDTO;
import com.learn.projeto_learn.dto.paciente.PatientResponseDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.service.Agendamento.AgendamentoService;
import com.learn.projeto_learn.service.medicalrecord.ProntuarioService;
import com.learn.projeto_learn.service.captcha.CaptchaService;
import com.learn.projeto_learn.service.patient.PatientAuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patient")
public class PatientPortalController {

    @Autowired private PatientAuthService patientAuthService;
    @Autowired private AgendamentoService agendamentoService;
    @Autowired private ProntuarioService prontuarioService;
    @Autowired private CaptchaService captchaService;


    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid PatientRegisterDTO data) {
        if (!captchaService.validate(data.captchaId(), data.captchaCode())) {
            throw new BusinessException("CAPTCHA inválido ou expirado.");
        }
        patientAuthService.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @GetMapping("/me")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<PatientResponseDTO> getProfile(@AuthenticationPrincipal Usuario user) {
        if (user.getPaciente() == null) {
            throw new BusinessException("Conta sem vínculo com paciente.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return ResponseEntity.ok(new PatientResponseDTO(user.getPaciente()));
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<List<AppointmentResponseDTO>> getMyAppointments(
            @AuthenticationPrincipal Usuario user) {
        if (user.getPaciente() == null) {
            throw new BusinessException("Conta sem vínculo com paciente.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return ResponseEntity.ok(agendamentoService.listByPaciente(user.getPaciente().getId()));
    }

    @GetMapping("/prontuarios")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<List<ProntuarioResponseDTO>> getMyProntuarios(
            @AuthenticationPrincipal Usuario user) {
        if (user.getPaciente() == null) {
            throw new BusinessException("Conta sem vínculo com paciente.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return ResponseEntity.ok(prontuarioService.listByPaciente(user.getPaciente().getId()));
    }
}
