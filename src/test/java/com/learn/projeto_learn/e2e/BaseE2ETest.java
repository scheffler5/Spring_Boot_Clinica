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
        String login = registrarELogarPaciente();

        // Completar perfil -> marketplace
        page.fill("#nome", "Paciente Teste E2E");
        page.fill("#cpf", gerarCpfValido());
        page.fill("#dataNascimento", "1990-05-10");
        page.fill("#nomeMae", "Mae Teste E2E");
        page.click("#form-profile button[type='submit']");
        page.waitForURL("**/patient-marketplace.html");

        return login;
    }

    /**
     * Registra um paciente novo e faz login, parando na tela de completar
     * perfil (perfil ainda incompleto). Retorna o login criado.
     */
    protected String registrarELogarPaciente() {
        String login = "pac_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000);
        String senha = "Senha@12345";

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

        return login;
    }

    /** Lê o token JWT guardado no localStorage da página atual. */
    protected String tokenAtual() {
        Object token = page.evaluate("() => localStorage.getItem('token')");
        return token == null ? null : token.toString();
    }

    /**
     * Semeia (de forma idempotente) um médico de teste com perfil completo,
     * especialidade e disponibilidade seg–sex 08:00–12:00, e limpa os
     * agendamentos dele (libera os horários a cada execução).
     * Retorna o id do médico. Conecta no banco via JDBC (env DB_URL/USER/PASS).
     */
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

            // Libera horários: remove agendamentos do médico de teste.
            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM tb_appointments WHERE medico_id = ?")) {
                del.setObject(1, medicoId);
                del.executeUpdate();
            }

            // Recria a disponibilidade seg–sex 08:00–12:00.
            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM tb_disponibilidade_medico WHERE medico_id = ?")) {
                del.setObject(1, medicoId);
                del.executeUpdate();
            }
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO tb_disponibilidade_medico " +
                    "(id, ativo, dia_semana, duracao_consulta_minutos, hora_fim, hora_inicio, medico_id) " +
                    "VALUES (?, true, ?, 60, '12:00', '08:00', ?)")) {
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
