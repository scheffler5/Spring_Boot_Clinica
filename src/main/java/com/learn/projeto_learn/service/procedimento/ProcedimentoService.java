package com.learn.projeto_learn.service.procedimento;

import com.learn.projeto_learn.dto.procedimento.ProcedimentoRequestDTO;
import com.learn.projeto_learn.dto.procedimento.ProcedimentoResponseDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.procedure.Procedimento;
import com.learn.projeto_learn.repository.ProcedimentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProcedimentoService {

    @Autowired
    private ProcedimentoRepository repository;

    public ProcedimentoResponseDTO create(ProcedimentoRequestDTO data) {
        Procedimento proc = new Procedimento();
        proc.setDescricao(data.descricao());
        proc.setCusto(data.custo());
        proc.setAtivo(true);
        return new ProcedimentoResponseDTO(repository.save(proc));
    }

    public List<ProcedimentoResponseDTO> listActive() {
        return repository.findAllByAtivo(true).stream()
                .map(ProcedimentoResponseDTO::new)
                .toList();
    }

    public ProcedimentoResponseDTO findById(UUID id) {
        return repository.findById(id)
                .map(ProcedimentoResponseDTO::new)
                .orElseThrow(() -> new BusinessException("Procedimento não encontrado.", HttpStatus.NOT_FOUND));
    }

    public ProcedimentoResponseDTO update(UUID id, ProcedimentoRequestDTO data) {
        Procedimento proc = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Procedimento não encontrado.", HttpStatus.NOT_FOUND));
        proc.setDescricao(data.descricao());
        proc.setCusto(data.custo());
        return new ProcedimentoResponseDTO(repository.save(proc));
    }

    public void deactivate(UUID id) {
        Procedimento proc = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Procedimento não encontrado.", HttpStatus.NOT_FOUND));
        proc.setAtivo(false);
        repository.save(proc);
    }
}
