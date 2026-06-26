package com.learn.projeto_learn.e2e;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

class PatientBookingE2ETest extends BaseE2ETest {

    static UUID medicoId;

    @BeforeAll
    static void seed() {
        medicoId = seedMedicoDeTeste();
    }

    @Test
    void pacienteAgendaConsultaEVeEmMinhaArea() {
        onboardAteMarketplace();

        page.locator(".doctor-card", new com.microsoft.playwright.Page.LocatorOptions()
                .setHasText("Dr. Teste E2E")).first().click();
        page.waitForURL("**/doctor-view.html**");

        LocalDate proximaSegunda = LocalDate.now().plusDays(1);
        while (proximaSegunda.getDayOfWeek() != DayOfWeek.MONDAY) {
            proximaSegunda = proximaSegunda.plusDays(1);
        }
        page.fill("#book-date", proximaSegunda.toString());

        page.locator(".slot-btn").first().click();
        page.click("#btn-agendar");

        page.click("#_c_sim");

        assertThat(page.locator("#book-message")).containsText("agendada");

        page.navigate(BASE_URL + "/patient-dashboard.html");
        assertThat(page.locator("#appointments-container")).containsText("Dr. Teste E2E");
    }
}
