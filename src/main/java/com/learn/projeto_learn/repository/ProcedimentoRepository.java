package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.model.procedure.Procedimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProcedimentoRepository extends JpaRepository<Procedimento, UUID> {
    List<Procedimento> findAllByAtivo(Boolean ativo);
}
