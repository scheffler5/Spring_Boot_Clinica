package com.learn.projeto_learn.controller.chat;

import com.learn.projeto_learn.dto.chat.ContatoDTO;
import com.learn.projeto_learn.dto.chat.ConversaDTO;
import com.learn.projeto_learn.dto.chat.MensagemDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.Imagem;
import com.learn.projeto_learn.model.user.Usuario;
import com.learn.projeto_learn.repository.ImagemRepository;
import com.learn.projeto_learn.service.chat.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
@Tag(name = "Chat", description = "Conversas, mensagens e upload de anexos (REST). O envio em tempo real é via WebSocket/STOMP")
public class ChatRestController {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    @Autowired private ChatService chatService;
    @Autowired private ImagemRepository imagemRepository;

    @GetMapping("/contatos")
    @Operation(summary = "Lista os contatos disponíveis para conversar")
    public ResponseEntity<List<ContatoDTO>> contatos(@AuthenticationPrincipal Usuario user) {
        return ResponseEntity.ok(chatService.listarContatos(user));
    }

    @PostMapping("/conversas/com/{contatoId}")
    @Operation(summary = "Cria ou recupera a conversa com um contato")
    public ResponseEntity<ConversaDTO> criarOuObter(@PathVariable UUID contatoId,
                                                     @AuthenticationPrincipal Usuario user) {
        return ResponseEntity.ok(chatService.criarOuObterConversa(user, contatoId));
    }

    @GetMapping("/conversas")
    @Operation(summary = "Lista as conversas do usuário autenticado")
    public ResponseEntity<List<ConversaDTO>> listar(@AuthenticationPrincipal Usuario user) {
        return ResponseEntity.ok(chatService.listarConversas(user));
    }

    @GetMapping("/conversas/{id}/mensagens")
    @Operation(summary = "Lista as mensagens de uma conversa")
    public ResponseEntity<List<MensagemDTO>> mensagens(@PathVariable String id,
                                                        @AuthenticationPrincipal Usuario user) {
        return ResponseEntity.ok(chatService.getMensagens(id, user.getId()));
    }

    @PatchMapping("/conversas/{id}/lidas")
    @Operation(summary = "Marca as mensagens de uma conversa como lidas")
    public ResponseEntity<Void> marcarLidas(@PathVariable String id,
                                             @AuthenticationPrincipal Usuario user) {
        chatService.marcarLidas(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Faz upload de um anexo do chat",
            description = "Aceita imagens (JPEG, PNG, WebP, GIF) ou documentos (PDF, DOC, DOCX) de até 10 MB.")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("arquivo") MultipartFile arquivo,
            @AuthenticationPrincipal Usuario user) throws IOException {

        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("Arquivo não pode ser vazio.");
        }
        String contentType = arquivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new BusinessException("Tipo de arquivo não suportado. Envie imagens (JPEG, PNG, WebP, GIF) ou documentos (PDF, DOC, DOCX).");
        }
        if (arquivo.getSize() > 10L * 1024 * 1024) {
            throw new BusinessException("Arquivo muito grande. Máximo 10 MB.");
        }

        String nome = arquivo.getOriginalFilename();
        Imagem imagem = imagemRepository.save(new Imagem(contentType, nome, arquivo.getBytes()));
        String url = "/imagens/" + imagem.getId();

        return ResponseEntity.ok(Map.of(
                "url",    url,
                "nome",   nome != null ? nome : "arquivo",
                "tipo",   contentType.startsWith("image/") ? "imagem" : "arquivo"
        ));
    }
}
