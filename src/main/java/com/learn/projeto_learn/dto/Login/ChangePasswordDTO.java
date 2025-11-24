package com.learn.projeto_learn.dto.Login;

import jakarta.validation.constraints.NotNull;

public record ChangePasswordDTO(
        @NotNull String email,
        @NotNull String code,
        @NotNull String newPassword
) {
}
