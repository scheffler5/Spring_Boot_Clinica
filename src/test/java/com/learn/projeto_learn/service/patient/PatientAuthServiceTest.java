package com.learn.projeto_learn.service.patient;

import com.learn.projeto_learn.dto.patient.PatientResponseDTO;
import com.learn.projeto_learn.dto.patient.CompleteProfileDTO;
import com.learn.projeto_learn.exception.BusinessException;
import com.learn.projeto_learn.model.user.Usuario;
import com.learn.projeto_learn.model.patient.Paciente;
import com.learn.projeto_learn.repository.PacienteRepository;
import com.learn.projeto_learn.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientAuthServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PacienteRepository pacienteRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks PatientAuthService service;

    private static final String CPF = "52998224725";
    private static final LocalDate NASC = LocalDate.of(1990, 5, 10);

    private Usuario usuarioNovo() {
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setLogin("paciente1");
        u.setPerfilCompleto(false);
        return u;
    }

    private Paciente pacienteExistente(String nome, LocalDate nascimento) {
        Paciente p = new Paciente();
        p.setId(UUID.randomUUID());
        p.setNome(nome);
        p.setCpf(CPF);
        p.setDataNascimento(nascimento);
        return p;
    }

    private CompleteProfileDTO dto(String nome) {
        return new CompleteProfileDTO(nome, CPF, NASC, "Maria Mãe");
    }

    @Test
    void cpfNovoCriaPacienteEVinculaConta() {
        Usuario user = usuarioNovo();
        when(pacienteRepository.findByCpf(CPF)).thenReturn(Optional.empty());
        when(pacienteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PatientResponseDTO resp = service.completeProfile(user, dto("João Silva"));

        assertEquals("João Silva", resp.nome());
        assertTrue(user.getPerfilCompleto());
        assertNotNull(user.getPaciente());
        verify(pacienteRepository).save(any());
    }

    @Test
    void cpfExistenteComDadosBatendoReaproveitaPaciente() {
        Usuario user = usuarioNovo();
        Paciente existente = pacienteExistente("João Silva", NASC);
        when(pacienteRepository.findByCpf(CPF)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByPaciente(existente)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PatientResponseDTO resp = service.completeProfile(user, dto("  joão silva "));

        assertEquals(existente.getId(), resp.id());
        assertEquals("Maria Mãe", existente.getNomeMae());
        assertSame(existente, user.getPaciente());
        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void cpfExistenteComNomeDiferenteRetorna422() {
        Usuario user = usuarioNovo();
        Paciente existente = pacienteExistente("Outro Nome", NASC);
        when(pacienteRepository.findByCpf(CPF)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByPaciente(existente)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.completeProfile(user, dto("João Silva")));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
    }

    @Test
    void cpfJaVinculadoAOutraContaRetorna409() {
        Usuario user = usuarioNovo();
        Paciente existente = pacienteExistente("João Silva", NASC);
        Usuario outroDono = new Usuario();
        outroDono.setId(UUID.randomUUID());
        when(pacienteRepository.findByCpf(CPF)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByPaciente(existente)).thenReturn(Optional.of(outroDono));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.completeProfile(user, dto("João Silva")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void perfilJaCompletoRetorna409() {
        Usuario user = usuarioNovo();
        user.setPerfilCompleto(true);
        user.setPaciente(pacienteExistente("João Silva", NASC));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.completeProfile(user, dto("João Silva")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }
}
