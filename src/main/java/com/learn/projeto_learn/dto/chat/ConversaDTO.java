package com.learn.projeto_learn.dto.chat;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversaDTO(
        UUID id,
        String outroUsuarioId,
        String outroUsuarioNome,
        String outroUsuarioFotoUrl,
        String ultimaMensagem,
        LocalDateTime ultimaMensagemEm,
        long naoLidas
) {}
