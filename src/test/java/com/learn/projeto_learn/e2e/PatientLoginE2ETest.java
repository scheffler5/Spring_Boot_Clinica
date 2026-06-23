package com.learn.projeto_learn.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Teste E2E de fumaça: garante que o portal do paciente carrega e que o
 * formulário de login está presente. Não sobe o Spring — apenas navega até
 * o app que já está rodando (URL via env var BASE_URL).
 */
class PatientLoginE2ETest {

    // Default para execução local; no container passamos BASE_URL apontando para o app.
    private static final String BASE_URL =
            System.getenv().getOrDefault("BASE_URL", "http://localhost:8080");

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
    void portalDoPacienteCarregaComFormularioDeLogin() {
        page.navigate(BASE_URL + "/patient-login.html");

        // Título da aba
        assertThat(page).hasTitle("Clínica — Portal do Paciente");

        // Campos do formulário de login visíveis
        assertThat(page.locator("#login-user")).isVisible();
        assertThat(page.locator("#login-pass")).isVisible();

        // Botão de entrar começa desabilitado (depende do captcha)
        assertThat(page.locator("#btn-login-submit")).isDisabled();
    }
}
