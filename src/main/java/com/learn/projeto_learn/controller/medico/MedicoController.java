package com.learn.projeto_learn.controller.medico;

import com.learn.projeto_learn.dto.agendamento.AppointmentResponseDTO;
import com.learn.projeto_learn.dto.medico.*;
import com.learn.projeto_learn.model.User.Especialidade;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.repository.AgendamentoRepository;
import com.learn.projeto_learn.repository.UsuarioRepository;

import com.learn.projeto_learn.service.medico.DisponibilidadeMedicoService;
import com.learn.projeto_learn.service.medico.MedicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/medico")
@Tag(name = "Portal do Médico", description = "Perfil, estatísticas, agenda e disponibilidades do médico")
public class MedicoController {

    @Autowired private MedicoService              medicoService;
    @Autowired private DisponibilidadeMedicoService disponibilidadeService;
    @Autowired private AgendamentoRepository       agendamentoRepository;
    @Autowired private UsuarioRepository           usuarioRepository;
    @Autowired private com.learn.projeto_learn.repository.ImagemRepository imagemRepository;

    @GetMapping("/especialidades")
    @Operation(summary = "Lista as especialidades médicas (valor/rótulo)",
            description = "Endpoint público usado nos formulários de perfil.",
            security = {})
    public ResponseEntity<List<Map<String, String>>> listarEspecialidades() {
        List<Map<String, String>> lista = Arrays.stream(Especialidade.values())
                .map(e -> Map.of("value", e.name(), "label", e.getDescricao()))
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/perfil")
    @PreAuthorize("hasRole('MEDIC')")
    @Operation(summary = "Atualiza o perfil do médico autenticado")
    public ResponseEntity<MedicoResponseDTO> atualizarPerfil(
            @AuthenticationPrincipal Usuario medico,
            @RequestBody @Valid MedicoProfileDTO data) {
        return ResponseEntity.ok(medicoService.completarPerfil(medico.getId(), data));
    }

    @PatchMapping("/perfil")
    @PreAuthorize("hasRole('MEDIC')")
    @Operation(summary = "Completa o perfil do médico (parcial)")
    public ResponseEntity<MedicoResponseDTO> completarPerfil(
            @AuthenticationPrincipal Usuario medico,
            @RequestBody @Valid MedicoProfileDTO data) {
        return ResponseEntity.ok(medicoService.completarPerfil(medico.getId(), data));
    }

    @GetMapping("/perfil")
    @PreAuthorize("hasRole('MEDIC')")
    @Operation(summary = "Retorna o perfil do médico autenticado")
    public ResponseEntity<MedicoResponseDTO> buscarPerfil(@AuthenticationPrincipal Usuario medico) {
        return ResponseEntity.ok(medicoService.buscarPerfil(medico.getId()));
    }

    @GetMapping("/estatisticas")
    @PreAuthorize("hasRole('MEDIC')")
    @Operation(summary = "Estatísticas do médico no mês",
            description = "Parâmetro 'mes' no formato yyyy-MM; usa o mês atual se omitido.")
    public ResponseEntity<MedicoEstatisticasDTO> getEstatisticas(
            @AuthenticationPrincipal Usuario medico,
            @RequestParam(required = false) String mes) {
        YearMonth yearMonth = (mes != null) ? YearMonth.parse(mes) : YearMonth.now();
        return ResponseEntity.ok(medicoService.getEstatisticas(medico.getId(), yearMonth));
    }

    @PostMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('MEDIC')")
    @Operation(summary = "Envia a foto de perfil do médico",
            description = "Aceita JPEG, PNG ou WebP de até 10 MB (multipart/form-data).")
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

        com.learn.projeto_learn.model.Imagem imagem =
                imagemRepository.save(new com.learn.projeto_learn.model.Imagem(contentType, arquivo.getBytes()));

        String url = "/imagens/" + imagem.getId();

        Usuario medico = usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new com.learn.projeto_learn.exception.BusinessException("Médico não encontrado.",
                        org.springframework.http.HttpStatus.NOT_FOUND));
        medico.setFotoUrl(url);

        return ResponseEntity.ok(new MedicoResponseDTO(usuarioRepository.save(medico)));
    }

    @GetMapping("/agenda")
    @PreAuthorize("hasRole('MEDIC')")
    @Operation(summary = "Lista a agenda de consultas do médico autenticado")
    public ResponseEntity<List<AppointmentResponseDTO>> getAgenda(@AuthenticationPrincipal Usuario medico) {
        List<AppointmentResponseDTO> agenda = agendamentoRepository
                .findAllByMedicoId(medico.getId()).stream()
                .map(AppointmentResponseDTO::new)
                .toList();
        return ResponseEntity.ok(agenda);
    }

    @PostMapping("/disponibilidade")
    @PreAuthorize("hasRole('MEDIC')")
    @Operation(summary = "Adiciona uma janela de disponibilidade")
    public ResponseEntity<DisponibilidadeResponseDTO> adicionarDisponibilidade(
            @AuthenticationPrincipal Usuario medico,
            @RequestBody @Valid DisponibilidadeRequestDTO data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disponibilidadeService.adicionar(medico.getId(), data));
    }

    @GetMapping("/disponibilidade")
    @PreAuthorize("hasRole('MEDIC')")
    @Operation(summary = "Lista as disponibilidades do médico autenticado")
    public ResponseEntity<List<DisponibilidadeResponseDTO>> listarDisponibilidade(
            @AuthenticationPrincipal Usuario medico) {
        return ResponseEntity.ok(disponibilidadeService.listar(medico.getId()));
    }

    @DeleteMapping("/disponibilidade/{id}")
    @PreAuthorize("hasRole('MEDIC')")
    @Operation(summary = "Remove uma janela de disponibilidade")
    public ResponseEntity<Void> removerDisponibilidade(
            @AuthenticationPrincipal Usuario medico,
            @PathVariable UUID id) {
        disponibilidadeService.remover(medico.getId(), id);
        return ResponseEntity.noContent().build();
    }

}
