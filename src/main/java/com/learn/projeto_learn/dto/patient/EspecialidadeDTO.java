package com.learn.projeto_learn.dto.patient;

import com.learn.projeto_learn.model.User.Especialidade;

public record EspecialidadeDTO(String nome, String descricao) {
    public EspecialidadeDTO(Especialidade e) {
        this(e.name(), e.getDescricao());
    }
}
