package com.learn.projeto_learn.service.marketplace;

import com.learn.projeto_learn.dto.agendamento.AppointmentResponseDTO;
import com.learn.projeto_learn.dto.patient.BookingRequestDTO;
import com.learn.projeto_learn.dto.patient.EspecialidadeDTO;
import com.learn.projeto_learn.dto.patient.MedicoMarketplaceDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.User.Especialidade;
import com.learn.projeto_learn.model.User.UserRole;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.model.agendamento.Agendamento;
import com.learn.projeto_learn.model.agendamento.DisponibilidadeMedico;
import com.learn.projeto_learn.model.agendamento.StatusAgendamento;
import com.learn.projeto_learn.model.patient.Paciente;
import com.learn.projeto_learn.repository.AgendamentoRepository;
import com.learn.projeto_learn.repository.DisponibilidadeMedicoRepository;
import com.learn.projeto_learn.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MarketplaceService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private DisponibilidadeMedicoRepository disponibilidadeRepository;

    // Fallback: horário comercial seg-sex, slots de 1h, quando o médico não configurou agenda.
    private static final LocalTime DEFAULT_INICIO = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_FIM = LocalTime.of(18, 0);
    private static final int DEFAULT_DURACAO_MIN = 60;

    public List<EspecialidadeDTO> listEspecialidades() {
        return Arrays.stream(Especialidade.values())
                .map(EspecialidadeDTO::new)
                .toList();
    }

    public List<MedicoMarketplaceDTO> listMedicos(Especialidade especialidade) {
        List<Usuario> medicos = (especialidade == null)
                ? usuarioRepository.findAllByRoleAndAtivoTrue(UserRole.MEDIC)
                : usuarioRepository.findAllByRoleAndEspecialidadeAndAtivoTrue(UserRole.MEDIC, especialidade);

        return medicos.stream()
                .filter(m -> m.getEspecialidade() != null) // somente médicos com perfil completo
                .map(MedicoMarketplaceDTO::new)
                .toList();
    }

    public List<LocalDateTime> getAvailableSlots(UUID medicoId, LocalDate data) {
        Usuario medico = usuarioRepository.findById(medicoId)
                .filter(m -> m.getRole() == UserRole.MEDIC && Boolean.TRUE.equals(m.getAtivo()))
                .orElseThrow(() -> new BusinessException("Médico não encontrado.", HttpStatus.NOT_FOUND));

        DayOfWeek dia = data.getDayOfWeek();
        List<DisponibilidadeMedico> disponibilidades =
                disponibilidadeRepository.findAllByMedicoIdAndDiaSemanaAndAtivoTrue(medico.getId(), dia);

        List<LocalDateTime> slots = new ArrayList<>();
        if (!disponibilidades.isEmpty()) {
            for (DisponibilidadeMedico d : disponibilidades) {
                gerarSlots(slots, data, d.getHoraInicio(), d.getHoraFim(), d.getDuracaoConsultaMinutos());
            }
        } else if (dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY) {
            gerarSlots(slots, data, DEFAULT_INICIO, DEFAULT_FIM, DEFAULT_DURACAO_MIN);
        }

        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.atTime(LocalTime.MAX);
        Set<LocalDateTime> ocupados = agendamentoRepository
                .findAllByMedicoIdAndDataHoraBetween(medico.getId(), inicioDia, fimDia)
                .stream()
                .filter(a -> a.getStatus() != StatusAgendamento.CANCELADO)
                .map(Agendamento::getDataHora)
                .collect(Collectors.toSet());

        LocalDateTime agora = LocalDateTime.now();
        return slots.stream()
                .distinct()
                .filter(s -> !ocupados.contains(s))
                .filter(s -> s.isAfter(agora))
                .sorted()
                .toList();
    }

    private void gerarSlots(List<LocalDateTime> destino, LocalDate data,
                            LocalTime inicio, LocalTime fim, int duracaoMin) {
        LocalTime t = inicio;
        while (!t.plusMinutes(duracaoMin).isAfter(fim)) {
            destino.add(LocalDateTime.of(data, t));
            t = t.plusMinutes(duracaoMin);
        }
    }

    @Transactional
    public AppointmentResponseDTO book(Usuario user, BookingRequestDTO data) {
        Paciente paciente = user.getPaciente();
        if (paciente == null) {
            throw new BusinessException("Complete seu perfil antes de agendar uma consulta.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Usuario medico = usuarioRepository.findById(data.medicoId())
                .filter(m -> m.getRole() == UserRole.MEDIC && Boolean.TRUE.equals(m.getAtivo()))
                .orElseThrow(() -> new BusinessException("Médico não encontrado.", HttpStatus.NOT_FOUND));

        if (data.dataHora().isBefore(LocalDateTime.now())) {
            throw new BusinessException("O horário selecionado já passou.");
        }

        boolean disponivel = getAvailableSlots(medico.getId(), data.dataHora().toLocalDate())
                .contains(data.dataHora());
        if (!disponivel) {
            throw new BusinessException("Horário indisponível para este médico.", HttpStatus.CONFLICT);
        }

        Agendamento agendamento = new Agendamento(paciente, medico, data.dataHora());
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        return new AppointmentResponseDTO(agendamentoRepository.save(agendamento));
    }
}
