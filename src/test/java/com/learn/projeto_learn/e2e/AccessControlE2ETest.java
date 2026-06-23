package com.learn.projeto_learn.e2e;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Controle de acesso (P1): garante que um PACIENTE autenticado não consegue
 * acessar rotas de staff (/appointments) nem de admin (/auth/users), e que
 * requisições sem token são rejeitadas.
 *
 * Usa o APIRequestContext do Playwright para chamadas HTTP diretas, com o
 * token real obtido pelo onboarding do paciente.
 */
class AccessControlE2ETest extends BaseE2ETest {

    @Test
    void pacienteNaoAcessaRotasDeStaffNemAdmin() {
        onboardAteMarketplace();

        // Token JWT real do paciente (guardado no localStorage pelo front).
        Object tokenObj = page.evaluate("() => localStorage.getItem('token')");
        assertNotNull(tokenObj, "Token do paciente deveria estar no localStorage");
        String token = tokenObj.toString();

        APIRequestContext autenticado = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(BASE_URL)
                        .setExtraHTTPHeaders(Map.of("Authorization", "Bearer " + token)));

        // Positivo: a própria rota do paciente funciona.
        assertEquals(200, autenticado.get("/patient/me").status(),
                "Paciente deve acessar a própria rota /patient/me");

        // Negativo: rota de staff (lista todos os agendamentos) deve ser barrada.
        assertEquals(403, autenticado.get("/appointments").status(),
                "Paciente NÃO deve acessar /appointments (rota de staff)");

        // Negativo: rota exclusiva de admin.
        assertEquals(403, autenticado.get("/auth/users").status(),
                "Paciente NÃO deve acessar /auth/users (rota de admin)");

        autenticado.dispose();

        // Sem token: deve exigir autenticação.
        APIRequestContext anonimo = playwright.request().newContext(
                new APIRequest.NewContextOptions().setBaseURL(BASE_URL));
        assertEquals(401, anonimo.get("/patient/me").status(),
                "Sem token deve retornar 401");
        anonimo.dispose();
    }
}
