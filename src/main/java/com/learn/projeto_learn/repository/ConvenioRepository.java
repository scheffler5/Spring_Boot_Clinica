package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.Domain.insurance.Convenio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConvenioRepository extends JpaRepository<Convenio, UUID> {}
