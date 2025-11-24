package com.learn.projeto_learn.dto.Login;

import jakarta.validation.constraints.NotNull;

public record ValidationCodeDTO(
        @NotNull String email,
        @NotNull String code
) {
}
