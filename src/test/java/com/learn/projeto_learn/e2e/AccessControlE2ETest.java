package com.learn.projeto_learn.e2e;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AccessControlE2ETest extends BaseE2ETest {

    @Test
    void pacienteNaoAcessaRotasDeStaffNemAdmin() {
        onboardAteMarketplace();

        Object tokenObj = page.evaluate("() => localStorage.getItem('token')");
        assertNotNull(tokenObj, "Token do paciente deveria estar no localStorage");
        String token = tokenObj.toString();

        APIRequestContext autenticado = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(BASE_URL)
                        .setExtraHTTPHeaders(Map.of("Authorization", "Bearer " + token)));

        assertEquals(200, autenticado.get("/patient/me").status(),
                "Paciente deve acessar a própria rota /patient/me");

        assertEquals(403, autenticado.get("/appointments").status(),
                "Paciente NÃO deve acessar /appointments (rota de staff)");

        assertEquals(403, autenticado.get("/auth/users").status(),
                "Paciente NÃO deve acessar /auth/users (rota de admin)");

        autenticado.dispose();

        APIRequestContext anonimo = playwright.request().newContext(
                new APIRequest.NewContextOptions().setBaseURL(BASE_URL));
        assertEquals(401, anonimo.get("/patient/me").status(),
                "Sem token deve retornar 401");
        anonimo.dispose();
    }
}
