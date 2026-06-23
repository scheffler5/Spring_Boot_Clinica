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

import java.util.concurrent.ThreadLocalRandom;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Base dos testes E2E: ciclo de vida do Playwright (Chromium headless) e
 * utilitários comuns (captcha PoW, geração de CPF, onboarding do paciente).
 */
abstract class BaseE2ETest {

    protected static final String BASE_URL =
            System.getenv().getOrDefault("BASE_URL", "http://localhost:8080");

    // Tempo para o worker resolver o proof-of-work e habilitar o botão.
    protected static final double CAPTCHA_TIMEOUT_MS = 30_000;

    static Playwright playwright;
    static Browser browser;
    protected Page page;

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

    /** Clica na caixa do captcha e espera o worker habilitar o botão de submit. */
    protected void resolverCaptcha(String captchaBox, String submitButton) {
        page.click(captchaBox);
        assertThat(page.locator(submitButton))
                .isEnabled(new LocatorAssertions.IsEnabledOptions().setTimeout(CAPTCHA_TIMEOUT_MS));
    }

    /**
     * Registra um paciente novo, faz login e completa o perfil, deixando a
     * página no marketplace. Retorna o login criado.
     */
    protected String onboardAteMarketplace() {
        String login = "pac_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000);
        String senha = "Senha@12345";
        String cpf   = gerarCpfValido();

        // Registro
        page.navigate(BASE_URL + "/patient-login.html");
        page.click(".tab[data-tab='register']");
        page.fill("#reg-user", login);
        page.fill("#reg-pass", senha);
        resolverCaptcha("#captcha-box-reg", "#btn-reg-submit");
        page.click("#btn-reg-submit");
        page.waitForSelector("#form-login",
                new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        // Login
        page.fill("#login-user", login);
        page.fill("#login-pass", senha);
        resolverCaptcha("#captcha-box-login", "#btn-login-submit");
        page.click("#btn-login-submit");
        page.waitForURL("**/patient-profile-complete.html");

        // Completar perfil
        page.fill("#nome", "Paciente Teste E2E");
        page.fill("#cpf", cpf);
        page.fill("#dataNascimento", "1990-05-10");
        page.fill("#nomeMae", "Mae Teste E2E");
        page.click("#form-profile button[type='submit']");
        page.waitForURL("**/patient-marketplace.html");

        return login;
    }

    /** Gera um CPF válido (com dígitos verificadores) para o teste ser idempotente. */
    protected static String gerarCpfValido() {
        int[] n = new int[11];
        for (int i = 0; i < 9; i++) n[i] = ThreadLocalRandom.current().nextInt(10);

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
