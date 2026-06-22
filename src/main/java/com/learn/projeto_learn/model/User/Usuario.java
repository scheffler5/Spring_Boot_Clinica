package com.learn.projeto_learn.model.User;

import com.learn.projeto_learn.model.patient.Paciente;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

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

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", unique = true, nullable = true)
    private Paciente paciente;

    @Column(length = 6)
    private String twoFactorToken;

    private LocalDateTime recoveryTokenExpiration;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean emailVerified = false;

    @Column(length = 6)
    private String emailVerificationToken;

    private LocalDateTime emailVerificationExpiration;

    @Column(length = 6)
    private String mfaToken;

    private LocalDateTime mfaTokenExpiration;


    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean ativo = false;

    @Column(columnDefinition = "TEXT")
    private String ssoToken;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Usuario(String login, String email, String password, UserRole role) {
        this.login = login;
        this.email = email;
        this.password = password;
        this.role = role;
        this.ativo = true;
        this.emailVerified = true;
    }

    public Usuario(String login, String email, String password, UserRole role, Paciente paciente) {
        this(login, email, password, role);
        this.paciente = paciente;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
        if (this.role == UserRole.ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_MEDIC"));
            authorities.add(new SimpleGrantedAuthority("ROLE_RECEPCIONIST"));
        }
        return authorities;
    }

    @Override public String getUsername()              { return login; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return Boolean.TRUE.equals(this.ativo); }
}
