package com.learn.projeto_learn.dto.Login;

import com.learn.projeto_learn.Domain.User.Usuario;

import java.util.UUID;

public record UserResponseDTO(UUID id, String login, String email, String role) {
    public UserResponseDTO(Usuario user){
        this(user.getId(), user.getLogin(), user.getEmail(), user.getRole().getRole());
    }
}
