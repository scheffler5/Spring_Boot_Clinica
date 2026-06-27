package com.learn.projeto_learn.controller.patient;

import com.learn.projeto_learn.dto.Login.PatientRegisterDTO;
import com.learn.projeto_learn.dto.agendamento.AppointmentResponseDTO;
import com.learn.projeto_learn.dto.patient.PatientResponseDTO;
import com.learn.projeto_learn.dto.patient.BookingRequestDTO;
import com.learn.projeto_learn.dto.patient.CompleteProfileDTO;
import com.learn.projeto_learn.dto.patient.EspecialidadeDTO;
import com.learn.projeto_learn.dto.patient.MedicoMarketplaceDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.Imagem;
import com.learn.projeto_learn.model.User.Especialidade;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.repository.ImagemRepository;
import com.learn.projeto_learn.service.Agendamento.AgendamentoService;
import com.learn.projeto_learn.service.marketplace.MarketplaceService;
import com.learn.projeto_learn.service.captcha.CaptchaService;
import com.learn.projeto_learn.service.patient.PatientAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patient")
@Tag(name = "Portal do Paciente", description = "Auto-cadastro, perfil, marketplace de médicos e agendamentos do paciente")
public class PatientPortalController {

    @Autowired private PatientAuthService patientAuthService;
    @Autowired private AgendamentoService agendamentoService;
    @Autowired private CaptchaService captchaService;
    @Autowired private MarketplaceService marketplaceService;
    @Autowired private com.learn.projeto_learn.repository.UsuarioRepository usuarioRepository;
    @Autowired private com.learn.projeto_learn.repository.DisponibilidadeMedicoRepository disponibilidadeRepository;
    @Autowired private ImagemRepository imagemRepository;

    @PostMapping("/register")
    @Operation(summary = "Auto-cadastro de paciente",
            description = "Cria uma conta de paciente após validar o CAPTCHA. Endpoint público.",
            security = {})
    public ResponseEntity<Void> register(@RequestBody @Valid PatientRegisterDTO data) {
        if (!captchaService.validate(data.captchaId(), data.captchaCode())) {
            throw new BusinessException("CAPTCHA inválido ou expirado.");
        }
        patientAuthService.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Retorna o perfil do paciente autenticado")
    public ResponseEntity<PatientResponseDTO> getProfile(@AuthenticationPrincipal Usuario user) {
        if (user.getPaciente() == null) {
            throw new BusinessException("Conta sem vínculo com paciente.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return ResponseEntity.ok(new PatientResponseDTO(user.getPaciente(), user.getFotoUrl()));
    }

    @PostMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Envia a foto de perfil do paciente",
            description = "Aceita JPEG, PNG ou WebP de até 10 MB (multipart/form-data).")
    public ResponseEntity<PatientResponseDTO> uploadFoto(
            @AuthenticationPrincipal Usuario principal,
            @RequestParam("arquivo") MultipartFile arquivo) throws IOException {

        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("Arquivo não pode ser vazio.");
        }
        String contentType = arquivo.getContentType();
        if (contentType == null || !Set.of("image/jpeg", "image/png", "image/webp").contains(contentType)) {
            throw new BusinessException("Apenas JPEG, PNG e WebP são aceitos.");
        }
        if (arquivo.getSize() > 10L * 1024 * 1024) {
            throw new BusinessException("Imagem muito grande. Máximo 10 MB.");
        }

        Imagem imagem = imagemRepository.save(new Imagem(contentType, arquivo.getBytes()));
        String url = "/imagens/" + imagem.getId();

        Usuario paciente = usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado.", HttpStatus.NOT_FOUND));
        paciente.setFotoUrl(url);
        usuarioRepository.save(paciente);

        return ResponseEntity.ok(new PatientResponseDTO(paciente.getPaciente(), url));
    }

    @PutMapping("/complete-profile")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Completa o cadastro do paciente",
            description = "Preenche os dados obrigatórios do perfil após o auto-cadastro.")
    public ResponseEntity<PatientResponseDTO> completeProfile(
            @AuthenticationPrincipal Usuario user,
            @RequestBody @Valid CompleteProfileDTO data) {
        return ResponseEntity.ok(patientAuthService.completeProfile(user, data));
    }

    @GetMapping("/especialidades")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Lista as especialidades disponíveis no marketplace")
    public ResponseEntity<List<EspecialidadeDTO>> listEspecialidades() {
        return ResponseEntity.ok(marketplaceService.listEspecialidades());
    }

    @GetMapping("/medicos")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Busca médicos no marketplace",
            description = "Filtra opcionalmente por especialidade e cidade.")
    public ResponseEntity<List<MedicoMarketplaceDTO>> listMedicos(
            @RequestParam(required = false) Especialidade especialidade,
            @RequestParam(required = false) String cidade) {
        return ResponseEntity.ok(marketplaceService.listMedicos(especialidade, cidade));
    }

    @GetMapping("/medicos/{medicoId}/horarios")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Lista horários livres de um médico em uma data")
    public ResponseEntity<List<LocalDateTime>> getHorarios(
            @PathVariable UUID medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(marketplaceService.getAvailableSlots(medicoId, data));
    }

    @PostMapping("/agendamentos")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Agenda uma consulta",
            description = "O paciente marca um horário disponível de um médico.")
    public ResponseEntity<AppointmentResponseDTO> book(
            @AuthenticationPrincipal Usuario user,
            @RequestBody @Valid BookingRequestDTO data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marketplaceService.book(user, data));
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Lista os agendamentos do paciente autenticado")
    public ResponseEntity<List<AppointmentResponseDTO>> getMyAppointments(
            @AuthenticationPrincipal Usuario user) {
        if (user.getPaciente() == null) {
            throw new BusinessException("Conta sem vínculo com paciente.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return ResponseEntity.ok(agendamentoService.listByPaciente(user.getPaciente().getId()));
    }

    @GetMapping("/medicos/{id}/detalhes")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Detalha um médico e suas disponibilidades")
    public ResponseEntity<com.learn.projeto_learn.dto.medico.MedicoDetalhesDTO> getMedicoDetalhes(
            @PathVariable java.util.UUID id) {
        com.learn.projeto_learn.model.User.Usuario medico = usuarioRepository.findById(id)
                .filter(m -> m.getRole() == com.learn.projeto_learn.model.User.UserRole.MEDIC
                          && Boolean.TRUE.equals(m.getAtivo())
                          && Boolean.TRUE.equals(m.getPerfilCompleto()))
                .orElseThrow(() -> new BusinessException("Médico não encontrado.", HttpStatus.NOT_FOUND));

        var disp = disponibilidadeRepository.findAllByMedicoIdAndAtivoTrue(id).stream()
                .map(com.learn.projeto_learn.dto.medico.DisponibilidadeResponseDTO::new)
                .toList();

        return ResponseEntity.ok(new com.learn.projeto_learn.dto.medico.MedicoDetalhesDTO(medico, disp));
    }
}
