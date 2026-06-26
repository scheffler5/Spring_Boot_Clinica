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

class PatientLoginE2ETest {

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

        assertThat(page).hasTitle("Clínica — Portal do Paciente");

        assertThat(page.locator("#login-user")).isVisible();
        assertThat(page.locator("#login-pass")).isVisible();

        assertThat(page.locator("#btn-login-submit")).isDisabled();
    }
}
