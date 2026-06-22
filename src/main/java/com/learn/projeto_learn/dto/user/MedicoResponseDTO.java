package com.learn.projeto_learn.dto.user;

import com.learn.projeto_learn.model.User.Usuario;

import java.util.UUID;

public record MedicoResponseDTO(
        UUID id,
        String login,
        String email
) {
    public MedicoResponseDTO(Usuario u) {
        this(u.getId(), u.getLogin(), u.getEmail());
    }
}
