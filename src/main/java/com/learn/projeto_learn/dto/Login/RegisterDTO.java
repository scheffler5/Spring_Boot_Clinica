package com.learn.projeto_learn.dto.Login;

import com.learn.projeto_learn.Domain.User.UserRole;

public record RegisterDTO(String login, String email, String password, UserRole role) {
}
