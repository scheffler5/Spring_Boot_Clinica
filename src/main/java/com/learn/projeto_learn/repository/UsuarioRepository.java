package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.Domain.User.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    UserDetails findByLogin(String login);
    boolean existsByEmail(String email);
    Optional<Usuario> findByEmail(String email);
}