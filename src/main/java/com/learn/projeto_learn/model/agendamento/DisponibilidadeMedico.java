package com.learn.projeto_learn.model.agendamento;

import com.learn.projeto_learn.model.User.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Table(name = "tb_disponibilidade_medico")
@Entity(name = "disponibilidade_medico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class DisponibilidadeMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Usuario medico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DayOfWeek diaSemana;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFim;

    @Column(nullable = false)
    private Integer duracaoConsultaMinutos = 60;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public DisponibilidadeMedico(Usuario medico, DayOfWeek diaSemana,
                                  LocalTime horaInicio, LocalTime horaFim,
                                  Integer duracaoConsultaMinutos) {
        this.medico = medico;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.duracaoConsultaMinutos = duracaoConsultaMinutos;
        this.ativo = true;
    }
}
