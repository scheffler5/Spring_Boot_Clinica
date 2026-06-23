package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.model.chat.Conversa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversaRepository extends JpaRepository<Conversa, UUID> {

    Optional<Conversa> findByPacienteIdAndMedicoId(UUID pacienteId, UUID medicoId);

    List<Conversa> findAllByPacienteIdOrderByUltimaMensagemEmDesc(UUID pacienteId);

    List<Conversa> findAllByMedicoIdOrderByUltimaMensagemEmDesc(UUID medicoId);
}
