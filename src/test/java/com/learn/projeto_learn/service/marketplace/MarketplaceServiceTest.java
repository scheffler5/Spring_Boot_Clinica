package com.learn.projeto_learn.service.marketplace;

import com.learn.projeto_learn.dto.agendamento.AppointmentResponseDTO;
import com.learn.projeto_learn.dto.patient.BookingRequestDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.User.UserRole;
import com.learn.projeto_learn.model.User.Usuario;
import com.learn.projeto_learn.model.agendamento.Agendamento;
import com.learn.projeto_learn.model.agendamento.DisponibilidadeMedico;
import com.learn.projeto_learn.model.agendamento.StatusAgendamento;
import com.learn.projeto_learn.model.patient.Paciente;
import com.learn.projeto_learn.repository.AgendamentoRepository;
import com.learn.projeto_learn.repository.DisponibilidadeMedicoRepository;
import com.learn.projeto_learn.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários (base da pirâmide) da lógica de agendamento.
 * Sem Spring, sem banco, sem navegador — repositórios mockados.
 */
@ExtendWith(MockitoExtension.class)
class MarketplaceServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock AgendamentoRepository agendamentoRepository;
    @Mock DisponibilidadeMedicoRepository disponibilidadeRepository;

    @InjectMocks MarketplaceService service;

    // Data futura para garantir que todos os slots fiquem "no futuro".
    private final LocalDate DATA = LocalDate.now().plusDays(7);

    private Usuario medico(UUID id, Integer duracao) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setLogin("med_teste");
        u.setNome("Dr. Teste");
        u.setRole(UserRole.MEDIC);
        u.setAtivo(true);
        u.setDuracaoConsultaMinutos(duracao);
        return u;
    }

    private DisponibilidadeMedico disp(LocalTime inicio, LocalTime fim) {
        DisponibilidadeMedico d = new DisponibilidadeMedico();
        d.setHoraInicio(inicio);
        d.setHoraFim(fim);
        d.setAtivo(true);
        return d;
    }

    private Agendamento agendamento(Usuario medico, LocalDateTime dataHora, StatusAgendamento status) {
        Paciente p = new Paciente();
        p.setId(UUID.randomUUID());
        p.setNome("Paciente X");
        Agendamento a = new Agendamento(p, medico, dataHora);
        a.setStatus(status);
        return a;
    }

    private Usuario pacienteLogado() {
        Usuario user = new Usuario();
        Paciente p = new Paciente();
        p.setId(UUID.randomUUID());
        p.setNome("Paciente Logado");
        user.setPaciente(p);
        return user;
    }

    @Test
    void geraSlotsDaDisponibilidade() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(medico(id, 60)));
        when(disponibilidadeRepository.findAllByMedicoIdAndDiaSemanaAndAtivoTrue(eq(id), any()))
                .thenReturn(List.of(disp(LocalTime.of(8, 0), LocalTime.of(12, 0))));
        when(agendamentoRepository.findAllByMedicoIdAndDataHoraBetween(eq(id), any(), any()))
                .thenReturn(List.of());

        List<LocalDateTime> slots = service.getAvailableSlots(id, DATA);

        assertEquals(
                List.of(DATA.atTime(8, 0), DATA.atTime(9, 0), DATA.atTime(10, 0), DATA.atTime(11, 0)),
                slots);
    }

    @Test
    void semDisponibilidadeRetornaListaVazia() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(medico(id, 60)));
        when(disponibilidadeRepository.findAllByMedicoIdAndDiaSemanaAndAtivoTrue(eq(id), any()))
                .thenReturn(List.of());

        assertTrue(service.getAvailableSlots(id, DATA).isEmpty());
    }

    @Test
    void slotOcupadoEhRemovido() {
        UUID id = UUID.randomUUID();
        Usuario med = medico(id, 60);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(med));
        when(disponibilidadeRepository.findAllByMedicoIdAndDiaSemanaAndAtivoTrue(eq(id), any()))
                .thenReturn(List.of(disp(LocalTime.of(8, 0), LocalTime.of(12, 0))));
        when(agendamentoRepository.findAllByMedicoIdAndDataHoraBetween(eq(id), any(), any()))
                .thenReturn(List.of(agendamento(med, DATA.atTime(9, 0), StatusAgendamento.CONFIRMADO)));

        List<LocalDateTime> slots = service.getAvailableSlots(id, DATA);

        assertFalse(slots.contains(DATA.atTime(9, 0)), "09:00 está ocupado e não deve aparecer");
        assertTrue(slots.contains(DATA.atTime(8, 0)));
    }

    @Test
    void slotDeAgendamentoCanceladoContinuaDisponivel() {
        UUID id = UUID.randomUUID();
        Usuario med = medico(id, 60);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(med));
        when(disponibilidadeRepository.findAllByMedicoIdAndDiaSemanaAndAtivoTrue(eq(id), any()))
                .thenReturn(List.of(disp(LocalTime.of(8, 0), LocalTime.of(12, 0))));
        when(agendamentoRepository.findAllByMedicoIdAndDataHoraBetween(eq(id), any(), any()))
                .thenReturn(List.of(agendamento(med, DATA.atTime(9, 0), StatusAgendamento.CANCELADO)));

        assertTrue(service.getAvailableSlots(id, DATA).contains(DATA.atTime(9, 0)),
                "Horário de um agendamento CANCELADO deve voltar a ficar livre");
    }

    @Test
    void bookSemPerfilDePacienteRetorna422() {
        Usuario semPaciente = new Usuario();
        BookingRequestDTO dto = new BookingRequestDTO(UUID.randomUUID(), DATA.atTime(8, 0));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.book(semPaciente, dto));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
    }

    @Test
    void bookHorarioIndisponivelRetorna409() {
        UUID id = UUID.randomUUID();
        Usuario med = medico(id, 60);
        LocalDateTime dataHora = DATA.atTime(8, 0);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(med));
        when(disponibilidadeRepository.findAllByMedicoIdAndDiaSemanaAndAtivoTrue(eq(id), any()))
                .thenReturn(List.of(disp(LocalTime.of(8, 0), LocalTime.of(12, 0))));
        when(agendamentoRepository.findAllByMedicoIdAndDataHoraBetween(eq(id), any(), any()))
                .thenReturn(List.of(agendamento(med, dataHora, StatusAgendamento.CONFIRMADO)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.book(pacienteLogado(), new BookingRequestDTO(id, dataHora)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void bookComSucessoCriaAgendamentoConfirmado() {
        UUID id = UUID.randomUUID();
        Usuario med = medico(id, 60);
        LocalDateTime dataHora = DATA.atTime(8, 0);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(med));
        when(disponibilidadeRepository.findAllByMedicoIdAndDiaSemanaAndAtivoTrue(eq(id), any()))
                .thenReturn(List.of(disp(LocalTime.of(8, 0), LocalTime.of(12, 0))));
        when(agendamentoRepository.findAllByMedicoIdAndDataHoraBetween(eq(id), any(), any()))
                .thenReturn(List.of());
        when(agendamentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AppointmentResponseDTO resp = service.book(pacienteLogado(), new BookingRequestDTO(id, dataHora));

        assertEquals(StatusAgendamento.CONFIRMADO, resp.status());
        assertEquals(dataHora, resp.dataHora());
        verify(agendamentoRepository).save(any());
    }
}
