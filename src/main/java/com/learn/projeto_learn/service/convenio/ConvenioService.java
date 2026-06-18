package com.learn.projeto_learn.service.convenio;

import com.learn.projeto_learn.dto.convenio.ConvenioRequestDTO;
import com.learn.projeto_learn.dto.convenio.ConvenioResponseDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.insurance.Convenio;
import com.learn.projeto_learn.repository.ConvenioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ConvenioService {

    @Autowired
    private ConvenioRepository repository;

    public ConvenioResponseDTO create(ConvenioRequestDTO data) {
        if (repository.existsByNome(data.nome())) {
            throw new BusinessException("Já existe um convênio com este nome.");
        }
        Convenio convenio = new Convenio();
        convenio.setNome(data.nome());
        convenio.setDesconto(data.desconto());
        convenio.setAtivo(true);
        return new ConvenioResponseDTO(repository.save(convenio));
    }

    public List<ConvenioResponseDTO> listActive() {
        return repository.findAllByAtivo(true).stream()
                .map(ConvenioResponseDTO::new)
                .toList();
    }

    public ConvenioResponseDTO findById(UUID id) {
        return repository.findById(id)
                .map(ConvenioResponseDTO::new)
                .orElseThrow(() -> new BusinessException("Convênio não encontrado.", HttpStatus.NOT_FOUND));
    }

    public ConvenioResponseDTO update(UUID id, ConvenioRequestDTO data) {
        Convenio convenio = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Convênio não encontrado.", HttpStatus.NOT_FOUND));
        convenio.setNome(data.nome());
        convenio.setDesconto(data.desconto());
        return new ConvenioResponseDTO(repository.save(convenio));
    }

    public void deactivate(UUID id) {
        Convenio convenio = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Convênio não encontrado.", HttpStatus.NOT_FOUND));
        convenio.setAtivo(false);
        repository.save(convenio);
    }
}
