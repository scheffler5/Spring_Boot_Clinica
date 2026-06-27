package com.learn.projeto_learn.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI clinicaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API da Clínica")
                        .version("0.0.1")
                        .description("""
                                API REST do sistema de gestão de clínica médica.

                                Cobre autenticação (JWT), portal do paciente, portal do médico,
                                agendamentos, prontuários, convênios, procedimentos e chat.

                                ## Autenticação
                                A maioria dos endpoints exige um token JWT no cabeçalho
                                `Authorization: Bearer <token>`. Para obter o token, use
                                `POST /auth/login` (médico/admin) ou autentique-se
                                como paciente. Clique em **Authorize** e informe apenas o token
                                (sem o prefixo `Bearer`).

                                ## Papéis (roles)
                                - `ADMIN` — administração geral (herda permissões de MEDIC)
                                - `MEDIC` — médico
                                - `PACIENTE` — paciente (portal próprio)
                                """)
                        .contact(new Contact().name("Projeto Final — Paradigmas"))
                        .license(new License().name("Uso acadêmico")))
                .servers(List.of(new Server().url("/").description("Servidor atual")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtido no login. Informe apenas o token, sem o prefixo Bearer.")));
    }
}
