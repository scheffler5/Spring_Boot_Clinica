package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.Domain.procedure.Procedimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcedimentoRepository extends JpaRepository<Procedimento, UUID> {}
