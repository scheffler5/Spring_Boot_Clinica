package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.Domain.medicalrecord.Prontuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProntuarioRepository extends JpaRepository<Prontuario, UUID> {}