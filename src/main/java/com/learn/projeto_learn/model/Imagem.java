package com.learn.projeto_learn.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "tb_imagens")
@Entity(name = "imagens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Imagem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String contentType;

    @Column(length = 255)
    private String nomeArquivo;

    @Column(nullable = false)
    private byte[] dados;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public Imagem(String contentType, byte[] dados) {
        this.contentType = contentType;
        this.dados       = dados;
    }

    public Imagem(String contentType, String nomeArquivo, byte[] dados) {
        this.contentType  = contentType;
        this.nomeArquivo  = nomeArquivo;
        this.dados        = dados;
    }
}
