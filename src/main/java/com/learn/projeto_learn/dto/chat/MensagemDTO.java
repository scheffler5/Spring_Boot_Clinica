package com.learn.projeto_learn.dto.chat;

import com.learn.projeto_learn.model.chat.Mensagem;

import java.time.LocalDateTime;

public record MensagemDTO(
        String id,
        String conversaId,
        String remetenteId,
        String remetenteNome,
        String tipoRemetente,
        String texto,
        String imagemUrl,
        String nomeAnexo,
        LocalDateTime timestamp,
        boolean lida
) {
    public MensagemDTO(Mensagem m) {
        this(m.getId(), m.getConversaId(), m.getRemetenteId(), m.getRemetenteNome(),
             m.getTipoRemetente(), m.getTexto(), m.getImagemUrl(), m.getNomeAnexo(),
             m.getTimestamp(), m.isLida());
    }
}
