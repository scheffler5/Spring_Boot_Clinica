package com.learn.projeto_learn.dto.Login;

public record LoginResponseDTO(
        String token,
        String role,
        boolean perfilCompleto
) {}
