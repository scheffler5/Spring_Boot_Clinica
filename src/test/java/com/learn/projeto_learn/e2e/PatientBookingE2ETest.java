package com.learn.projeto_learn.e2e;

import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Jornada de agendamento do paciente (fluxo crítico P0):
 *   marketplace -> escolher médico -> ver horários -> agendar -> confirmar em "Minha Área".
 *
 * Cobre os endpoints /patient/medicos, /patient/medicos/{id}/horarios e
 * /patient/agendamentos. Requer ao menos um médico com especialidade cadastrado.
 */
class PatientBookingE2ETest extends BaseE2ETest {

    @Test
    void pacienteAgendaConsultaEVeEmMinhaArea() {
        onboardAteMarketplace();

        // O book() do front usa confirm() — precisamos aceitar o diálogo.
        page.onDialog(dialog -> dialog.accept());

        // Deve haver médicos com especialidade no marketplace.
        assertThat(page.locator(".card-grid .card").first()).isVisible();

        // Abre os horários do primeiro médico e guarda o nome dele.
        page.getByRole(AriaRole.BUTTON, new com.microsoft.playwright.Page.GetByRoleOptions()
                .setName("Ver horários")).first().click();
        String nomeMedico = page.locator("#booking-medico-nome").textContent().trim();

        // Escolhe a próxima segunda-feira (dia útil garante slots no fallback comercial).
        LocalDate proximaSegunda = LocalDate.now().plusDays(1);
        while (proximaSegunda.getDayOfWeek() != DayOfWeek.MONDAY) {
            proximaSegunda = proximaSegunda.plusDays(1);
        }
        page.fill("#booking-data", proximaSegunda.toString());

        // Agenda o primeiro horário disponível.
        page.locator("#slots-container .slot-btn").first().click();

        // Mensagem de sucesso do agendamento.
        assertThat(page.locator("#message")).containsText("Consulta confirmada");

        // Confirma que aparece em "Minha Área" como CONFIRMADO.
        page.navigate(BASE_URL + "/patient-dashboard.html");
        assertThat(page.locator("#appointments-container")).containsText(nomeMedico);
        assertThat(page.locator("#appointments-container")).containsText("Confirmado");
    }
}
