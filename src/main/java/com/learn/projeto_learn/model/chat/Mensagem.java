package com.learn.projeto_learn.model.chat;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "mensagens")
public class Mensagem {

    @Id
    private String id;

    @Indexed
    private String conversaId;

    private String remetenteId;
    private String remetenteNome;
    private String tipoRemetente;  // "MEDICO" ou "PACIENTE"
    private String texto;

    private String imagemUrl;   // URL do anexo (/imagens/{uuid}), null se só texto
    private String nomeAnexo;   // Nome original do arquivo anexado

    @CreatedDate
    private LocalDateTime timestamp;

    private boolean lida = false;

    public Mensagem() {}

    public Mensagem(String conversaId, String remetenteId, String remetenteNome,
                    String tipoRemetente, String texto) {
        this.conversaId    = conversaId;
        this.remetenteId   = remetenteId;
        this.remetenteNome = remetenteNome;
        this.tipoRemetente = tipoRemetente;
        this.texto         = texto;
        this.timestamp     = LocalDateTime.now();
    }

    public Mensagem(String conversaId, String remetenteId, String remetenteNome,
                    String tipoRemetente, String texto, String imagemUrl, String nomeAnexo) {
        this.conversaId    = conversaId;
        this.remetenteId   = remetenteId;
        this.remetenteNome = remetenteNome;
        this.tipoRemetente = tipoRemetente;
        this.texto         = texto;
        this.imagemUrl     = imagemUrl;
        this.nomeAnexo     = nomeAnexo;
        this.timestamp     = LocalDateTime.now();
    }

    public String getId()             { return id; }
    public String getConversaId()     { return conversaId; }
    public String getRemetenteId()    { return remetenteId; }
    public String getRemetenteNome()  { return remetenteNome; }
    public String getTipoRemetente()  { return tipoRemetente; }
    public String getTexto()          { return texto; }
    public String getImagemUrl()      { return imagemUrl; }
    public String getNomeAnexo()      { return nomeAnexo; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isLida()           { return lida; }
    public void setLida(boolean lida) { this.lida = lida; }
}
