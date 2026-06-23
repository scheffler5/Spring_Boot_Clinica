package com.learn.projeto_learn.dto.medico;

import com.learn.projeto_learn.model.agendamento.DisponibilidadeMedico;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record DisponibilidadeResponseDTO(
        UUID id,
        DayOfWeek diaSemana,
        String diaSemanaLabel,
        LocalTime horaInicio,
        LocalTime horaFim,
        Boolean ativo
) {
    public DisponibilidadeResponseDTO(DisponibilidadeMedico d) {
        this(d.getId(), d.getDiaSemana(), traduzirDia(d.getDiaSemana()),
             d.getHoraInicio(), d.getHoraFim(), d.getAtivo());
    }

    private static String traduzirDia(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY    -> "Segunda-feira";
            case TUESDAY   -> "Terça-feira";
            case WEDNESDAY -> "Quarta-feira";
            case THURSDAY  -> "Quinta-feira";
            case FRIDAY    -> "Sexta-feira";
            case SATURDAY  -> "Sábado";
            case SUNDAY    -> "Domingo";
        };
    }
}
