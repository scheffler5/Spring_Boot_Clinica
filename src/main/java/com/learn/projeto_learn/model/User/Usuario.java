package com.learn.projeto_learn.model.User;

import com.learn.projeto_learn.model.patient.Paciente;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Table(name = "tb_users")
@Entity(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String login;

    @Column(length = 100)
    private String nome;

    @Column(unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean perfilCompleto = false;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "paciente_id", unique = true, nullable = true)
    private Paciente paciente;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Especialidade especialidade;

    @Column(length = 20)
    private String crm;

    @Column(length = 100)
    private String cidade;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(length = 150)
    private String universidade;

    @Column
    private Integer anoFormacao;

    @Column(columnDefinition = "TEXT")
    private String fotoUrl;

    @Column(precision = 10, scale = 2)
    private BigDecimal valorConsulta;

    @Column(columnDefinition = "INTEGER DEFAULT 60")
    private Integer duracaoConsultaMinutos = 60;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Usuario(String login, String senhaHash, UserRole role) {
        this.login = login;
        this.password = senhaHash;
        this.role = role;
        this.ativo = true;
        this.perfilCompleto = false;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
        return authorities;
    }

    @Override public String getUsername()              { return login; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return Boolean.TRUE.equals(this.ativo); }
}
