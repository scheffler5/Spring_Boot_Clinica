package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.Domain.location.Local;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LocalRepository extends JpaRepository<Local, UUID> {}
