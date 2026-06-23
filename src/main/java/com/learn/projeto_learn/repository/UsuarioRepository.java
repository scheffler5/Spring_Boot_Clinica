package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.model.User.Especialidade;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.model.User.UserRole;
import com.learn.projeto_learn.model.patient.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    UserDetails findByLogin(String login);
    boolean existsByEmail(String email);
    Optional<Usuario> findByEmail(String email);
    boolean existsByPaciente(Paciente paciente);
    Optional<Usuario> findByPaciente(Paciente paciente);
    List<Usuario> findAllByRoleInAndAtivoTrue(Collection<UserRole> roles);
    List<Usuario> findAllByRoleAndAtivoTrue(UserRole role);
    List<Usuario> findAllByRoleAndEspecialidadeAndAtivoTrue(UserRole role, Especialidade especialidade);
}
