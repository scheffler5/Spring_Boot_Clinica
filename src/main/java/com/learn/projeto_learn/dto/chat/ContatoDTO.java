package com.learn.projeto_learn.dto.chat;

public record ContatoDTO(
        String usuarioId,
        String nome,
        String fotoUrl,
        String tipo,
        String conversaId
) {}
