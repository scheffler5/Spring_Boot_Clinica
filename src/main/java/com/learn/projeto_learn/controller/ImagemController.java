package com.learn.projeto_learn.controller;

import com.learn.projeto_learn.model.Imagem;
import com.learn.projeto_learn.repository.ImagemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/imagens")
public class ImagemController {

    @Autowired
    private ImagemRepository imagemRepository;

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> servir(@PathVariable UUID id) {
        Imagem imagem = imagemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagem não encontrada."));

        var resp = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.getContentType()))
                .header("Cache-Control", "public, max-age=86400");

        if (!imagem.getContentType().startsWith("image/") && imagem.getNomeArquivo() != null) {
            resp = resp.header("Content-Disposition",
                    "attachment; filename=\"" + imagem.getNomeArquivo() + "\"");
        }

        return resp.body(imagem.getDados());
    }
}
