package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.model.insurance.Convenio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConvenioRepository extends JpaRepository<Convenio, UUID> {
    List<Convenio> findAllByAtivo(Boolean ativo);
    boolean existsByNome(String nome);
}
