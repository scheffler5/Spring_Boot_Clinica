package com.learn.projeto_learn.controller.patient;

import com.learn.projeto_learn.dto.Login.PatientRegisterDTO;
import com.learn.projeto_learn.dto.agendamento.AppointmentResponseDTO;
import com.learn.projeto_learn.dto.medicalrecord.ProntuarioResponseDTO;
import com.learn.projeto_learn.dto.paciente.PatientResponseDTO;
import com.learn.projeto_learn.dto.patient.BookingRequestDTO;
import com.learn.projeto_learn.dto.patient.CompleteProfileDTO;
import com.learn.projeto_learn.dto.patient.EspecialidadeDTO;
import com.learn.projeto_learn.dto.patient.MedicoMarketplaceDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.User.Especialidade;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.service.Agendamento.AgendamentoService;
import com.learn.projeto_learn.service.marketplace.MarketplaceService;
import com.learn.projeto_learn.service.medicalrecord.ProntuarioService;
import com.learn.projeto_learn.service.captcha.CaptchaService;
import com.learn.projeto_learn.service.patient.PatientAuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patient")
public class PatientPortalController {

    @Autowired private PatientAuthService patientAuthService;
    @Autowired private AgendamentoService agendamentoService;
    @Autowired private ProntuarioService prontuarioService;
    @Autowired private CaptchaService captchaService;
    @Autowired private MarketplaceService marketplaceService;


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

    @PutMapping("/complete-profile")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<PatientResponseDTO> completeProfile(
            @AuthenticationPrincipal Usuario user,
            @RequestBody @Valid CompleteProfileDTO data) {
        return ResponseEntity.ok(patientAuthService.completeProfile(user, data));
    }

    @GetMapping("/especialidades")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<List<EspecialidadeDTO>> listEspecialidades() {
        return ResponseEntity.ok(marketplaceService.listEspecialidades());
    }

    @GetMapping("/medicos")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<List<MedicoMarketplaceDTO>> listMedicos(
            @RequestParam(required = false) Especialidade especialidade) {
        return ResponseEntity.ok(marketplaceService.listMedicos(especialidade));
    }

    @GetMapping("/medicos/{medicoId}/horarios")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<List<LocalDateTime>> getHorarios(
            @PathVariable UUID medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(marketplaceService.getAvailableSlots(medicoId, data));
    }

    @PostMapping("/agendamentos")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<AppointmentResponseDTO> book(
            @AuthenticationPrincipal Usuario user,
            @RequestBody @Valid BookingRequestDTO data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marketplaceService.book(user, data));
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
