package com.learn.projeto_learn.e2e;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Jornada de agendamento do paciente (fluxo crítico P0) — atualizada para o
 * Fase D, em que o agendamento acontece na página doctor-view.html:
 *   marketplace -> abrir médico -> escolher data -> escolher horário ->
 *   confirmar -> ver em "Minha Área".
 *
 * Requer um médico com perfil completo + disponibilidade configurada
 * (semeado em @BeforeAll). Cobre /patient/medicos, /patient/medicos/{id}/horarios
 * e /patient/agendamentos.
 */
class PatientBookingE2ETest extends BaseE2ETest {

    static UUID medicoId;

    @BeforeAll
    static void seed() {
        medicoId = seedMedicoDeTeste();
    }

    @Test
    void pacienteAgendaConsultaEVeEmMinhaArea() {
        onboardAteMarketplace();

        // No marketplace, abre o médico de teste (navega para doctor-view.html).
        page.locator(".doctor-card", new com.microsoft.playwright.Page.LocatorOptions()
                .setHasText("Dr. Teste E2E")).first().click();
        page.waitForURL("**/doctor-view.html**");

        // Escolhe a próxima segunda-feira (há disponibilidade seg–sex).
        LocalDate proximaSegunda = LocalDate.now().plusDays(1);
        while (proximaSegunda.getDayOfWeek() != DayOfWeek.MONDAY) {
            proximaSegunda = proximaSegunda.plusDays(1);
        }
        page.fill("#book-date", proximaSegunda.toString());

        // Seleciona o primeiro horário disponível e agenda.
        page.locator(".slot-btn").first().click();
        page.click("#btn-agendar");

        // Overlay de confirmação custom (não é o confirm() nativo).
        page.click("#_c_sim");

        // Mensagem de sucesso do agendamento.
        assertThat(page.locator("#book-message")).containsText("agendada");

        // Confirma que aparece em "Minha Área".
        page.navigate(BASE_URL + "/patient-dashboard.html");
        assertThat(page.locator("#appointments-container")).containsText("Dr. Teste E2E");
    }
}
