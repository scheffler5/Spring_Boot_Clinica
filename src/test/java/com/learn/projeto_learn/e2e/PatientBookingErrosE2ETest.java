package com.learn.projeto_learn.e2e;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PatientBookingErrosE2ETest extends BaseE2ETest {

    static UUID medicoId;

    @BeforeAll
    static void seed() {
        medicoId = seedMedicoDeTeste();
    }

    private APIRequestContext apiComToken(String token) {
        return playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL(BASE_URL)
                .setExtraHTTPHeaders(Map.of("Authorization", "Bearer " + token)));
    }

    private RequestOptions corpoAgendamento(String dataHora) {
        return RequestOptions.create()
                .setHeader("Content-Type", "application/json")
                .setData("{\"medicoId\":\"" + medicoId + "\",\"dataHora\":\"" + dataHora + "\"}");
    }

    private static LocalDate proxima(DayOfWeek alvo) {
        LocalDate d = LocalDate.now().plusDays(1);
        while (d.getDayOfWeek() != alvo) d = d.plusDays(1);
        return d;
    }

    @Test
    void agendarHorarioJaOcupadoRetornaConflito() {
        onboardAteMarketplace();
        APIRequestContext api = apiComToken(tokenAtual());

        String dataHora = proxima(DayOfWeek.MONDAY) + "T08:00:00";

        assertEquals(201, api.post("/patient/agendamentos", corpoAgendamento(dataHora)).status(),
                "Primeiro agendamento deve ser criado");
        assertEquals(409, api.post("/patient/agendamentos", corpoAgendamento(dataHora)).status(),
                "Mesmo horário novamente deve dar conflito (409)");

        api.dispose();
    }

    @Test
    void diaSemDisponibilidadeNaoTemHorarios() {
        onboardAteMarketplace();
        APIRequestContext api = apiComToken(tokenAtual());

        LocalDate domingo = proxima(DayOfWeek.SUNDAY);
        APIResponse res = api.get("/patient/medicos/" + medicoId + "/horarios?data=" + domingo);

        assertEquals(200, res.status());
        assertEquals("[]", res.text().trim(), "Domingo não deve ter horários");

        api.dispose();
    }

    @Test
    void agendarSemPerfilCompletoRetorna422() {
        registrarELogarPaciente();
        APIRequestContext api = apiComToken(tokenAtual());

        String dataHora = proxima(DayOfWeek.MONDAY) + "T09:00:00";
        assertEquals(422, api.post("/patient/agendamentos", corpoAgendamento(dataHora)).status(),
                "Sem perfil completo o agendamento deve ser bloqueado (422)");

        api.dispose();
    }
}
