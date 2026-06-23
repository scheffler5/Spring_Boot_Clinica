package com.learn.projeto_learn.repository;

import com.learn.projeto_learn.model.chat.Mensagem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MensagemRepository extends MongoRepository<Mensagem, String> {

    List<Mensagem> findByConversaIdOrderByTimestampAsc(String conversaId);

    List<Mensagem> findByConversaIdOrderByTimestampDesc(String conversaId, Pageable pageable);

    long countByConversaIdAndRemetenteIdNotAndLidaFalse(String conversaId, String remetenteId);
}
