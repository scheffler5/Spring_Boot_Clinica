package com.learn.projeto_learn.dto.login;

public record LoginResponseDTO(
        String token,
        String role,
        boolean perfilCompleto
) {}
