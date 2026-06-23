package com.learn.projeto_learn.dto.medico;

import java.math.BigDecimal;

public record MedicoEstatisticasDTO(
        int totalAtendidosMes,
        int totalAgendadosMes,
        BigDecimal receitaRealizadaMes,
        BigDecimal receitaProjetadaMes
) {}
