package com.learn.projeto_learn.controller.chat;

import com.learn.projeto_learn.dto.chat.EnviarMensagemDTO;
import com.learn.projeto_learn.dto.chat.MensagemDTO;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWsController {

    @Autowired private ChatService chatService;
    @Autowired private SimpMessagingTemplate broker;

    @MessageMapping("/chat/{conversaId}")
    public void enviar(@DestinationVariable String conversaId,
                       @Payload EnviarMensagemDTO payload,
                       Principal principal) {

        if (payload == null) return;
        if (principal == null) return;
        boolean semTexto = payload.texto() == null || payload.texto().isBlank();
        boolean semAnexo = payload.imagemUrl() == null;
        if (semTexto && semAnexo) return;

        Usuario remetente = extrairUsuario(principal);
        if (remetente == null) return;

        String texto = payload.texto() != null ? payload.texto().trim() : "";
        if (texto.isBlank() && payload.imagemUrl() == null) return;

        MensagemDTO msg = chatService.salvar(conversaId, remetente, texto,
                payload.imagemUrl(), payload.nomeAnexo());
        broker.convertAndSend("/topic/conversa/" + conversaId, msg);
    }

    private Usuario extrairUsuario(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof Usuario usuario) {
            return usuario;
        }
        return null;
    }
}
