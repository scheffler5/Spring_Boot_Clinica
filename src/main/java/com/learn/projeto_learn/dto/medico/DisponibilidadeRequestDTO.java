package com.learn.projeto_learn.dto.medico;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DisponibilidadeRequestDTO(
        @NotNull(message = "Dia da semana é obrigatório")
        DayOfWeek diaSemana,

        @NotNull(message = "Hora de início é obrigatória")
        LocalTime horaInicio,

        @NotNull(message = "Hora de fim é obrigatória")
        LocalTime horaFim
) {}
