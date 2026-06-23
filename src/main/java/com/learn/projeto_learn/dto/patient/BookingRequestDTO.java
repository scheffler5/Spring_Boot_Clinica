package com.learn.projeto_learn.dto.patient;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingRequestDTO(
        @NotNull(message = "O médico é obrigatório")
        UUID medicoId,

        @NotNull(message = "A data e hora são obrigatórias")
        @Future(message = "O horário deve ser no futuro")
        LocalDateTime dataHora
) {}
