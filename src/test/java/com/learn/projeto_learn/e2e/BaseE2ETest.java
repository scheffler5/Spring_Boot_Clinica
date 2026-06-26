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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

abstract class BaseE2ETest {

    protected static final String BASE_URL =
            System.getenv().getOrDefault("BASE_URL", "http://localhost:8080");

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

    protected void resolverCaptcha(String captchaBox, String submitButton) {
        page.click(captchaBox);
        assertThat(page.locator(submitButton))
                .isEnabled(new LocatorAssertions.IsEnabledOptions().setTimeout(CAPTCHA_TIMEOUT_MS));
    }

    protected String onboardAteMarketplace() {
        String login = registrarELogarPaciente();

        page.fill("#nome", "Paciente Teste E2E");
        page.fill("#cpf", gerarCpfValido());
        page.fill("#dataNascimento", "1990-05-10");
        page.fill("#nomeMae", "Mae Teste E2E");
        page.click("#form-profile button[type='submit']");
        page.waitForURL("**/patient-marketplace.html");

        return login;
    }

    protected String registrarELogarPaciente() {
        String login = "pac_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000);
        String senha = "Senha@12345";

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

        return login;
    }

    protected String tokenAtual() {
        Object token = page.evaluate("() => localStorage.getItem('token')");
        return token == null ? null : token.toString();
    }

    protected static UUID seedMedicoDeTeste() {
        String url  = System.getenv().getOrDefault("DB_URL",  "jdbc:postgresql://localhost:5432/clinica");
        String user = System.getenv().getOrDefault("DB_USER", "clinica_user");
        String pass = System.getenv().getOrDefault("DB_PASS", "clinica_pass");
        String login = "medico_e2e";

        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            UUID medicoId;
            try (PreparedStatement ps = c.prepareStatement("SELECT id FROM tb_users WHERE login = ?")) {
                ps.setString(1, login);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        medicoId = (UUID) rs.getObject("id");
                        try (PreparedStatement up = c.prepareStatement(
                                "UPDATE tb_users SET role='MEDIC', ativo=true, perfil_completo=true, " +
                                "especialidade='CARDIOLOGIA', nome='Dr. Teste E2E', crm='99999/SP', " +
                                "valor_consulta=200, duracao_consulta_minutos=60 WHERE id = ?")) {
                            up.setObject(1, medicoId);
                            up.executeUpdate();
                        }
                    } else {
                        medicoId = UUID.randomUUID();
                        try (PreparedStatement ins = c.prepareStatement(
                                "INSERT INTO tb_users (id, login, password, role, ativo, email_verified, " +
                                "perfil_completo, nome, especialidade, crm, valor_consulta, duracao_consulta_minutos) " +
                                "VALUES (?, ?, 'x', 'MEDIC', true, true, true, 'Dr. Teste E2E', 'CARDIOLOGIA', " +
                                "'99999/SP', 200, 60)")) {
                            ins.setObject(1, medicoId);
                            ins.setString(2, login);
                            ins.executeUpdate();
                        }
                    }
                }
            }

            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM tb_appointments WHERE medico_id = ?")) {
                del.setObject(1, medicoId);
                del.executeUpdate();
            }

            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM tb_disponibilidade_medico WHERE medico_id = ?")) {
                del.setObject(1, medicoId);
                del.executeUpdate();
            }
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO tb_disponibilidade_medico " +
                    "(id, ativo, dia_semana, hora_fim, hora_inicio, medico_id) " +
                    "VALUES (?, true, ?, '12:00', '08:00', ?)")) {
                for (DayOfWeek d : new DayOfWeek[]{
                        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
                    ins.setObject(1, UUID.randomUUID());
                    ins.setString(2, d.name());
                    ins.setObject(3, medicoId);
                    ins.executeUpdate();
                }
            }

            return medicoId;
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao semear médico de teste", e);
        }
    }

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
