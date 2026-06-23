package com.learn.projeto_learn.dto.chat;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record IniciarConversaDTO(@NotNull UUID medicoId) {}
