package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.model.Imagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImagemRepository extends JpaRepository<Imagem, UUID> {}
