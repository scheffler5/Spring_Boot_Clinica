package com.learn.projeto_learn.controller.medico;

import com.learn.projeto_learn.dto.agendamento.AppointmentResponseDTO;
import com.learn.projeto_learn.dto.medico.*;
import com.learn.projeto_learn.model.User.Especialidade;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.repository.AgendamentoRepository;
import com.learn.projeto_learn.repository.UsuarioRepository;

import com.learn.projeto_learn.service.medico.DisponibilidadeMedicoService;
import com.learn.projeto_learn.service.medico.MedicoService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
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
    @Autowired private UsuarioRepository           usuarioRepository;
    @Autowired private com.learn.projeto_learn.repository.ImagemRepository imagemRepository;

    @GetMapping("/especialidades")
    public ResponseEntity<List<Map<String, String>>> listarEspecialidades() {
        List<Map<String, String>> lista = Arrays.stream(Especialidade.values())
                .map(e -> Map.of("value", e.name(), "label", e.getDescricao()))
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/perfil")
    @PreAuthorize("hasAnyRole('MEDIC', 'ADMIN')")
    public ResponseEntity<MedicoResponseDTO> atualizarPerfil(
            @AuthenticationPrincipal Usuario medico,
            @RequestBody @Valid MedicoProfileDTO data) {
        return ResponseEntity.ok(medicoService.completarPerfil(medico.getId(), data));
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

    @PostMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MEDIC', 'ADMIN')")
    public ResponseEntity<MedicoResponseDTO> uploadFoto(
            @AuthenticationPrincipal Usuario principal,
            @RequestParam("arquivo") org.springframework.web.multipart.MultipartFile arquivo) throws java.io.IOException {

        if (arquivo == null || arquivo.isEmpty()) {
            throw new com.learn.projeto_learn.exception.BusinessException("Arquivo não pode ser vazio.");
        }
        String contentType = arquivo.getContentType();
        if (contentType == null || !java.util.Set.of("image/jpeg","image/png","image/webp").contains(contentType)) {
            throw new com.learn.projeto_learn.exception.BusinessException("Apenas JPEG, PNG e WebP são aceitos.");
        }
        if (arquivo.getSize() > 10L * 1024 * 1024) {
            throw new com.learn.projeto_learn.exception.BusinessException("Imagem muito grande. Máximo 10 MB.");
        }

        // Salva a imagem na tabela tb_imagens (esquema S3-like)
        com.learn.projeto_learn.model.Imagem imagem =
                imagemRepository.save(new com.learn.projeto_learn.model.Imagem(contentType, arquivo.getBytes()));

        // URL estável que aponta para o endpoint /imagens/{id}
        String url = "/imagens/" + imagem.getId();

        // Recarrega o usuário do banco para evitar problemas de entidade desconectada
        Usuario medico = usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new com.learn.projeto_learn.exception.BusinessException("Médico não encontrado.",
                        org.springframework.http.HttpStatus.NOT_FOUND));
        medico.setFotoUrl(url);

        return ResponseEntity.ok(new MedicoResponseDTO(usuarioRepository.save(medico)));
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
