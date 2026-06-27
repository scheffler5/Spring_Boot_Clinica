package com.learn.projeto_learn.model.chat;

import com.learn.projeto_learn.model.user.Usuario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "tb_conversas",
       uniqueConstraints = @UniqueConstraint(columnNames = {"paciente_id", "medico_id"}))
@Entity(name = "conversas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Conversa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Usuario paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Usuario medico;

    @Column(columnDefinition = "TEXT")
    private String ultimaMensagem;

    @Column
    private LocalDateTime ultimaMensagemEm;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime criadaEm;

    public Conversa(Usuario paciente, Usuario medico) {
        this.paciente = paciente;
        this.medico   = medico;
    }
}
