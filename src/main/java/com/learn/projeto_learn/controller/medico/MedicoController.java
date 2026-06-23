package com.learn.projeto_learn.controller.medico;

import com.learn.projeto_learn.dto.agendamento.AppointmentResponseDTO;
import com.learn.projeto_learn.dto.medico.*;
import com.learn.projeto_learn.model.User.Especialidade;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.repository.AgendamentoRepository;
import com.learn.projeto_learn.service.medico.DisponibilidadeMedicoService;
import com.learn.projeto_learn.service.medico.MedicoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/medico")
public class MedicoController {

    @Autowired private MedicoService              medicoService;
    @Autowired private DisponibilidadeMedicoService disponibilidadeService;
    @Autowired private AgendamentoRepository       agendamentoRepository;

    @GetMapping("/especialidades")
    public ResponseEntity<List<Map<String, String>>> listarEspecialidades() {
        List<Map<String, String>> lista = Arrays.stream(Especialidade.values())
                .map(e -> Map.of("value", e.name(), "label", e.getDescricao()))
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PatchMapping("/perfil")
    @PreAuthorize("hasAnyRole('MEDIC', 'ADMIN')")
    public ResponseEntity<MedicoResponseDTO> completarPerfil(
            @AuthenticationPrincipal Usuario medico,
            @RequestBody @Valid MedicoProfileDTO data) {
        return ResponseEntity.ok(medicoService.completarPerfil(medico.getId(), data));
    }

    @GetMapping("/perfil")
    @PreAuthorize("hasAnyRole('MEDIC', 'ADMIN')")
    public ResponseEntity<MedicoResponseDTO> buscarPerfil(@AuthenticationPrincipal Usuario medico) {
        return ResponseEntity.ok(medicoService.buscarPerfil(medico.getId()));
    }

    @GetMapping("/estatisticas")
    @PreAuthorize("hasAnyRole('MEDIC', 'ADMIN')")
    public ResponseEntity<MedicoEstatisticasDTO> getEstatisticas(
            @AuthenticationPrincipal Usuario medico,
            @RequestParam(required = false) String mes) {
        YearMonth yearMonth = (mes != null) ? YearMonth.parse(mes) : YearMonth.now();
        return ResponseEntity.ok(medicoService.getEstatisticas(medico.getId(), yearMonth));
    }

    @GetMapping("/agenda")
    @PreAuthorize("hasAnyRole('MEDIC', 'ADMIN')")
    public ResponseEntity<List<AppointmentResponseDTO>> getAgenda(@AuthenticationPrincipal Usuario medico) {
        List<AppointmentResponseDTO> agenda = agendamentoRepository
                .findAllByMedicoId(medico.getId()).stream()
                .map(AppointmentResponseDTO::new)
                .toList();
        return ResponseEntity.ok(agenda);
    }

    @PostMapping("/disponibilidade")
    @PreAuthorize("hasAnyRole('MEDIC', 'ADMIN')")
    public ResponseEntity<DisponibilidadeResponseDTO> adicionarDisponibilidade(
            @AuthenticationPrincipal Usuario medico,
            @RequestBody @Valid DisponibilidadeRequestDTO data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disponibilidadeService.adicionar(medico.getId(), data));
    }

    @GetMapping("/disponibilidade")
    @PreAuthorize("hasAnyRole('MEDIC', 'ADMIN')")
    public ResponseEntity<List<DisponibilidadeResponseDTO>> listarDisponibilidade(
            @AuthenticationPrincipal Usuario medico) {
        return ResponseEntity.ok(disponibilidadeService.listar(medico.getId()));
    }

    @DeleteMapping("/disponibilidade/{id}")
    @PreAuthorize("hasAnyRole('MEDIC', 'ADMIN')")
    public ResponseEntity<Void> removerDisponibilidade(
            @AuthenticationPrincipal Usuario medico,
            @PathVariable UUID id) {
        disponibilidadeService.remover(medico.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/disponibilidade/slots")
    @PreAuthorize("hasAnyRole('MEDIC', 'ADMIN', 'PACIENTE')")
    public ResponseEntity<List<String>> gerarSlots(
            @RequestParam UUID medicoId,
            @RequestParam String data) {
        return ResponseEntity.ok(
                disponibilidadeService.gerarSlotsDisponiveis(medicoId, LocalDate.parse(data)));
    }
}
