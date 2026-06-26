package com.learn.projeto_learn.service;

import com.learn.projeto_learn.dto.chat.ContatoDTO;
import com.learn.projeto_learn.dto.chat.ConversaDTO;
import com.learn.projeto_learn.dto.chat.MensagemDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.User.UserRole;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.model.agendamento.Agendamento;
import com.learn.projeto_learn.model.agendamento.StatusAgendamento;
import com.learn.projeto_learn.model.chat.Conversa;
import com.learn.projeto_learn.model.chat.Mensagem;
import com.learn.projeto_learn.model.patient.Paciente;
import com.learn.projeto_learn.repository.AgendamentoRepository;
import com.learn.projeto_learn.repository.ConversaRepository;
import com.learn.projeto_learn.repository.MensagemRepository;
import com.learn.projeto_learn.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired private ConversaRepository  conversaRepository;
    @Autowired private MensagemRepository  mensagemRepository;
    @Autowired private UsuarioRepository   usuarioRepository;
    @Autowired private AgendamentoRepository agendamentoRepository;

    public List<ContatoDTO> listarContatos(Usuario usuario) {
        if (usuario.getRole() == UserRole.PACIENTE) {
            return contatosDoPaciente(usuario);
        } else {
            return contatosDoMedico(usuario);
        }
    }

    private List<ContatoDTO> contatosDoPaciente(Usuario usuario) {
        Paciente paciente = usuario.getPaciente();
        if (paciente == null) return List.of();

        Set<UUID> medicosVistos = new HashSet<>();
        return agendamentoRepository.findAllByPacienteId(paciente.getId()).stream()
                .filter(a -> a.getStatus() != StatusAgendamento.CANCELADO)
                .map(Agendamento::getMedico)
                .filter(m -> medicosVistos.add(m.getId()))
                .map(m -> toContatoDTO(m, "MEDICO", usuario))
                .toList();
    }

    private List<ContatoDTO> contatosDoMedico(Usuario medico) {
        Set<UUID> pacientesVistos = new HashSet<>();
        return agendamentoRepository.findAllByMedicoId(medico.getId()).stream()
                .filter(a -> a.getStatus() != StatusAgendamento.CANCELADO)
                .map(Agendamento::getPaciente)
                .filter(p -> pacientesVistos.add(p.getId()))
                .map(p -> {
                    Optional<Usuario> uOpt = usuarioRepository.findByPaciente(p);
                    return uOpt.map(u -> toContatoDTO(u, p.getNome(), "PACIENTE", medico))
                               .orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private ContatoDTO toContatoDTO(Usuario outro, String tipo, Usuario eu) {
        String nome = (outro.getNome() != null && !outro.getNome().isBlank())
                ? outro.getNome() : outro.getLogin();
        return toContatoDTO(outro, nome, tipo, eu);
    }

    private ContatoDTO toContatoDTO(Usuario outro, String nome, String tipo, Usuario eu) {
        String conversaId = null;
        UUID pacId = tipo.equals("MEDICO") ? eu.getId()    : outro.getId();
        UUID medId = tipo.equals("MEDICO") ? outro.getId() : eu.getId();
        Optional<Conversa> conv = conversaRepository.findByPacienteIdAndMedicoId(pacId, medId);
        if (conv.isPresent()) conversaId = conv.get().getId().toString();
        return new ContatoDTO(outro.getId().toString(), nome, outro.getFotoUrl(), tipo, conversaId);
    }

    @Transactional
    public ConversaDTO criarOuObterConversa(Usuario eu, UUID contatoId) {
        UUID pacienteUsuarioId, medicoId;

        if (eu.getRole() == UserRole.PACIENTE) {
            pacienteUsuarioId = eu.getId();
            medicoId          = contatoId;
        } else {
            pacienteUsuarioId = contatoId;
            medicoId          = eu.getId();
        }

        return conversaRepository.findByPacienteIdAndMedicoId(pacienteUsuarioId, medicoId)
                .map(c -> toDTO(c, eu.getId()))
                .orElseGet(() -> {
                    Usuario pUsr = usuarioRepository.findById(pacienteUsuarioId).orElseThrow();
                    Usuario mUsr = usuarioRepository.findById(medicoId).orElseThrow();
                    return toDTO(conversaRepository.save(new Conversa(pUsr, mUsr)), eu.getId());
                });
    }

    public List<ConversaDTO> listarConversas(Usuario user) {
        UUID id = user.getId();
        List<Conversa> lista = user.getRole() == UserRole.PACIENTE
                ? conversaRepository.findAllByPacienteIdOrderByUltimaMensagemEmDesc(id)
                : conversaRepository.findAllByMedicoIdOrderByUltimaMensagemEmDesc(id);
        return lista.stream().map(c -> toDTO(c, id)).toList();
    }

    public List<MensagemDTO> getMensagens(String conversaId, UUID userId) {
        validarParticipacao(conversaId, userId);
        return mensagemRepository.findByConversaIdOrderByTimestampAsc(conversaId).stream()
                .map(MensagemDTO::new).toList();
    }

    @Transactional
    public MensagemDTO salvar(String conversaId, Usuario remetente, String texto) {
        return salvar(conversaId, remetente, texto, null, null);
    }

    @Transactional
    public MensagemDTO salvar(String conversaId, Usuario remetente, String texto,
                              String imagemUrl, String nomeAnexo) {
        validarParticipacao(conversaId, remetente.getId());
        String tipo = remetente.getRole() == UserRole.PACIENTE ? "PACIENTE" : "MEDICO";
        String nome = (remetente.getNome() != null && !remetente.getNome().isBlank())
                ? remetente.getNome() : remetente.getLogin();

        Mensagem msg = mensagemRepository.save(
                new Mensagem(conversaId, remetente.getId().toString(), nome, tipo,
                             texto, imagemUrl, nomeAnexo));

        String preview = imagemUrl != null
                ? (nomeAnexo != null ? "📎 " + nomeAnexo : "📎 Anexo")
                : (texto != null && texto.length() > 80 ? texto.substring(0, 80) + "..." : texto);

        conversaRepository.findById(UUID.fromString(conversaId)).ifPresent(c -> {
            c.setUltimaMensagem(preview);
            c.setUltimaMensagemEm(LocalDateTime.now());
            conversaRepository.save(c);
        });
        return new MensagemDTO(msg);
    }

    @Transactional
    public void marcarLidas(String conversaId, UUID userId) {
        validarParticipacao(conversaId, userId);
        List<Mensagem> naoLidas = mensagemRepository
                .findByConversaIdOrderByTimestampAsc(conversaId).stream()
                .filter(m -> !m.isLida() && !m.getRemetenteId().equals(userId.toString()))
                .collect(Collectors.toList());
        naoLidas.forEach(m -> m.setLida(true));
        mensagemRepository.saveAll(naoLidas);
    }

    private void validarParticipacao(String conversaId, UUID userId) {
        Conversa c = conversaRepository.findById(UUID.fromString(conversaId))
                .orElseThrow(() -> new BusinessException("Conversa não encontrada.", HttpStatus.NOT_FOUND));
        if (!c.getPaciente().getId().equals(userId) && !c.getMedico().getId().equals(userId)) {
            throw new BusinessException("Acesso negado.", HttpStatus.FORBIDDEN);
        }
    }

    private ConversaDTO toDTO(Conversa c, UUID viewerId) {
        boolean viewerEhPaciente = c.getPaciente().getId().equals(viewerId);
        Usuario outro = viewerEhPaciente ? c.getMedico() : c.getPaciente();
        String  nome  = (outro.getNome() != null && !outro.getNome().isBlank())
                ? outro.getNome() : outro.getLogin();
        long naoLidas = mensagemRepository
                .countByConversaIdAndRemetenteIdNotAndLidaFalse(c.getId().toString(), viewerId.toString());
        return new ConversaDTO(c.getId(), outro.getId().toString(), nome,
                outro.getFotoUrl(), c.getUltimaMensagem(), c.getUltimaMensagemEm(), naoLidas);
    }
}
