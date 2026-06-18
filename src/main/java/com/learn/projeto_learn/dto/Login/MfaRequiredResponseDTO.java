package com.learn.projeto_learn.dto.Login;

public record MfaRequiredResponseDTO(
        boolean mfaRequired,
        String emailHint,
        String email
) {}
