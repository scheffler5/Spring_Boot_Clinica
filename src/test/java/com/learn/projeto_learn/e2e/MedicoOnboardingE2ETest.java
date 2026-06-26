package com.learn.projeto_learn.e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Jornada de onboarding do médico (P1, fluxo do Fase D):
 *   registrar -> login -> completar perfil médico -> configurar disponibilidade.
 *
 * Cobre /auth/register (MEDIC), /auth/login, PATCH /medico/perfil e
 * POST /medico/disponibilidade (com o GET que alimenta a lista).
 */
class MedicoOnboardingE2ETest extends BaseE2ETest {

    @Test
    void medicoRegistraCompletaPerfilEConfiguraDisponibilidade() {
        // Desliga o tour guiado da dashboard para não bloquear os cliques.
        page.addInitScript("localStorage.setItem('tour_done_medico-v1','true');");

        String login = "med_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000);
        String senha = "Senha@12345";

        // ── 1) Registro (acesso médico) ──────────────────────────────
        page.navigate(BASE_URL + "/index.html");
        page.click(".tab[data-tab='register']");
        page.fill("#reg-user", login);
        page.fill("#reg-pass", senha);
        resolverCaptcha("#captcha-box-reg", "#btn-reg-submit");
        page.click("#btn-reg-submit");
        page.waitForSelector("#form-login",
                new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        // ── 2) Login ─────────────────────────────────────────────────
        page.fill("#login-user", login);
        page.fill("#login-pass", senha);
        resolverCaptcha("#captcha-box-login", "#btn-login-submit");
        page.click("#btn-login-submit");
        page.waitForURL("**/doctor-profile-complete.html");

        // ── 3) Completar perfil médico ───────────────────────────────
        page.fill("#nome", "Dr. Onboard E2E");
        page.fill("#crm", "55555/SP");
        // selectOption já aguarda a opção (carregada via fetch) aparecer no select.
        page.selectOption("#especialidade", "CARDIOLOGIA");
        page.fill("#valorConsulta", "250");
        page.selectOption("#duracaoConsultaMinutos", "60");
        page.click("#form-profile button[type='submit']");
        page.waitForURL("**/dashboard.html");

        // ── 4) Configurar disponibilidade ────────────────────────────
        page.click(".nav-tab[data-section='disponibilidade']");
        page.selectOption("#dia-semana", "MONDAY");
        page.fill("#hora-inicio", "08:00");
        page.fill("#hora-fim", "12:00");
        page.click("#form-disponibilidade button[type='submit']");

        // A disponibilidade recém-criada aparece na lista.
        assertThat(page.locator("#disponibilidade-list")).containsText("08:00");
    }
}
