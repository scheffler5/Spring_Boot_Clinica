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

class PatientOnboardingE2ETest {

    private static final String BASE_URL =
            System.getenv().getOrDefault("BASE_URL", "http://localhost:8080");

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

        page.navigate(BASE_URL + "/patient-login.html");
        page.click(".tab[data-tab='register']");
        page.fill("#reg-user", login);
        page.fill("#reg-pass", senha);
        resolverCaptcha("#captcha-box-reg", "#btn-reg-submit");
        page.click("#btn-reg-submit");

        page.waitForSelector("#form-login",
                new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        page.fill("#login-user", login);
        page.fill("#login-pass", senha);
        resolverCaptcha("#captcha-box-login", "#btn-login-submit");
        page.click("#btn-login-submit");

        page.waitForURL("**/patient-profile-complete.html");

        page.fill("#nome", "Paciente Teste E2E");
        page.fill("#cpf", cpf);
        page.fill("#dataNascimento", "1990-05-10");
        page.fill("#nomeMae", "Mae Teste E2E");
        page.click("#form-profile button[type='submit']");

        page.waitForURL("**/patient-marketplace.html");
        assertThat(page.locator("#filtro-especialidade")).isVisible();
    }

    private void resolverCaptcha(String captchaBox, String submitButton) {
        page.click(captchaBox);
        assertThat(page.locator(submitButton))
                .isEnabled(new LocatorAssertions.IsEnabledOptions().setTimeout(CAPTCHA_TIMEOUT_MS));
    }

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
