package com.learn.projeto_learn.dto.chat;

public record ContatoDTO(
        String usuarioId,
        String nome,
        String fotoUrl,
        String tipo,        // "MEDICO" ou "PACIENTE"
        String conversaId   // null se ainda não há conversa
) {}
