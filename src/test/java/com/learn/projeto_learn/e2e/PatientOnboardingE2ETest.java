package com.learn.projeto_learn.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Jornada de onboarding do paciente (fluxo crítico P0):
 *   registrar -> login -> completar perfil -> chegar ao marketplace.
 *
 * Cobre os endpoints /patient/register, /auth/login e /patient/complete-profile
 * (incluindo a validação de identidade por CPF). O captcha PoW é resolvido pelo
 * próprio navegador ao clicar em "Não sou um robô".
 */
class PatientOnboardingE2ETest {

    private static final String BASE_URL =
            System.getenv().getOrDefault("BASE_URL", "http://localhost:8080");

    // Tempo para o worker resolver o proof-of-work e habilitar o botão.
    private static final double CAPTCHA_TIMEOUT_MS = 30_000;

    static Playwright playwright;
    static Browser browser;
    Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void closeBrowser() {
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void createPage() {
        page = browser.newContext().newPage();
    }

    @AfterEach
    void closePage() {
        if (page != null) page.close();
    }

    @Test
    void pacienteSeRegistraFazLoginCompletaPerfilEChegaAoMarketplace() {
        String login = "pac_" + System.currentTimeMillis();
        String senha = "Senha@12345";
        String cpf   = gerarCpfValido();

        // ── 1) Registro ──────────────────────────────────────────────
        page.navigate(BASE_URL + "/patient-login.html");
        page.click(".tab[data-tab='register']");
        page.fill("#reg-user", login);
        page.fill("#reg-pass", senha);
        resolverCaptcha("#captcha-box-reg", "#btn-reg-submit");
        page.click("#btn-reg-submit");

        // Sucesso volta para a aba de login (form-login fica visível de novo)
        page.waitForSelector("#form-login",
                new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        // ── 2) Login ─────────────────────────────────────────────────
        page.fill("#login-user", login);
        page.fill("#login-pass", senha);
        resolverCaptcha("#captcha-box-login", "#btn-login-submit");
        page.click("#btn-login-submit");

        // Perfil incompleto -> redireciona para completar cadastro
        page.waitForURL("**/patient-profile-complete.html");

        // ── 3) Completar perfil ──────────────────────────────────────
        page.fill("#nome", "Paciente Teste E2E");
        page.fill("#cpf", cpf);
        page.fill("#dataNascimento", "1990-05-10");
        page.fill("#nomeMae", "Mae Teste E2E");
        page.click("#form-profile button[type='submit']");

        // ── 4) Chega ao marketplace ──────────────────────────────────
        page.waitForURL("**/patient-marketplace.html");
        assertThat(page.locator("#filtro-especialidade")).isVisible();
    }

    /** Clica na caixa do captcha e espera o worker habilitar o botão de submit. */
    private void resolverCaptcha(String captchaBox, String submitButton) {
        page.click(captchaBox);
        assertThat(page.locator(submitButton))
                .isEnabled(new LocatorAssertions.IsEnabledOptions().setTimeout(CAPTCHA_TIMEOUT_MS));
    }

    /** Gera um CPF válido (com dígitos verificadores) para o teste ser idempotente. */
    private static String gerarCpfValido() {
        Random rnd = ThreadLocalRandom.current();
        int[] n = new int[11];
        for (int i = 0; i < 9; i++) n[i] = rnd.nextInt(10);

        for (int j = 9; j < 11; j++) {
            int soma = 0;
            for (int i = 0; i < j; i++) soma += n[i] * ((j + 1) - i);
            int resto = (soma * 10) % 11;
            n[j] = (resto == 10) ? 0 : resto;
        }

        StringBuilder sb = new StringBuilder();
        for (int d : n) sb.append(d);
        return sb.toString();
    }
}
