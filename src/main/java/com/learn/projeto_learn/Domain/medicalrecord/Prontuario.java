package com.learn.projeto_learn.Domain.medicalrecord;

import com.learn.projeto_learn.Domain.User.Usuario;
import com.learn.projeto_learn.Domain.insurance.Convenio;
import com.learn.projeto_learn.Domain.patient.Paciente;
import com.learn.projeto_learn.Domain.procedure.Procedimento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "tb_prontuarios")
@Entity(name = "prontuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Prontuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario medico;

    @ManyToOne
    @JoinColumn(name = "convenio_id", nullable = false)
    private Convenio convenio;

    @ManyToOne
    @JoinColumn(name = "procedimento_id", nullable = false)
    private Procedimento procedimento;
    @Column(nullable = false)
    private BigDecimal valorCalculado;

    @CreationTimestamp
    private LocalDateTime dataAtendimento;

    @Column(columnDefinition = "TEXT")
    private String observacoes;
}