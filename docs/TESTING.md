# Estratégia de Testes

Este documento descreve a estratégia de testes automatizados do projeto, como
executá-los e os resultados obtidos.

## Pirâmide de testes

A suíte segue o modelo de pirâmide: muitos testes rápidos e determinísticos na
base (unitários) e poucos testes de jornada completa no topo (E2E).

```
        ▲  E2E (Playwright)        8 testes  · ~18 s  · navegador + app + banco
       ▲▲▲ Unitários (Mockito)    12 testes  · <1 s   · sem app/banco/navegador
```

| Camada | Qtde | Tecnologia | Velocidade | O que valida |
|---|---|---|---|---|
| **Unitário** | 12 | JUnit 5 + Mockito | < 1 s | Lógica de negócio isolada (regras de agendamento, validação de identidade) |
| **E2E** | 8 | Playwright (Java) | ~18 s | Jornadas reais de ponta a ponta no navegador |

## Camada unitária (base)

Testes puros, sem Spring, banco ou navegador — repositórios são mockados.
Rápidos e determinísticos: rodam igual em qualquer máquina.

- **`MarketplaceServiceTest`** (7) — geração de slots a partir da disponibilidade,
  remoção de horário ocupado, liberação de horário de agendamento cancelado,
  ausência de disponibilidade → lista vazia, e o `book`: 422 (sem perfil),
  409 (conflito), sucesso → `CONFIRMADO`.
- **`PatientAuthServiceTest`** (5) — validação de identidade por CPF ao completar
  o perfil: CPF novo, dados batendo (case-insensitive), nome divergente → 422,
  CPF já vinculado → 409, perfil já completo → 409.

```bash
./scripts/run-unit.sh            # todos os *ServiceTest (~1 s)
```

## Camada E2E (topo)

Testes de interface com Playwright, executados contra a aplicação real em
execução. O captcha *proof-of-work* é resolvido pelo próprio navegador.

- **`PatientLoginE2ETest`** — smoke da tela de login.
- **`PatientOnboardingE2ETest`** — registrar → login → completar perfil (paciente).
- **`PatientBookingE2ETest`** — marketplace → médico → horários → agendar.
- **`PatientBookingErrosE2ETest`** — conflito (409), dia sem slots, sem perfil (422).
- **`MedicoOnboardingE2ETest`** — registrar → perfil médico → disponibilidade.
- **`AccessControlE2ETest`** — autorização por perfil (paciente não acessa rotas de staff/admin).

```bash
docker compose up -d     # app no ar
./scripts/run-e2e.sh             # todos os *E2ETest
```

### Infraestrutura E2E

- `Dockerfile.e2e` — imagem com **JDK 26** (do projeto) + **navegadores** (imagem
  oficial do Playwright). Resolve o fato de a imagem oficial trazer apenas JDK 21.
- `--network host` — faz a origem ser `localhost`, contexto seguro exigido pelo
  `crypto.subtle` do captcha PoW.

## Cobertura (JaCoCo)

```bash
./scripts/run-coverage.sh        # gera target/site/jacoco/index.html
```

Cobertura de instruções das classes exercitadas pelos unitários:

| Serviço | Cobertura |
|---|---|
| `PatientAuthService` | ~81% |
| `MarketplaceService` | ~70% |

> **Nota técnica:** o JaCoCo ainda não instrumenta o bytecode do Java 26
> (`Unsupported class file major version 70`). Como o código não usa recursos
> exclusivos do Java 26, a cobertura é gerada compilando em **Java 21** via o
> profile `-Pcoverage`. O build de produção permanece em Java 26.

## Integração contínua

O workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) roda em todo
push na `main` e em todo Pull Request:

- **unit-tests** (JDK 26) — gate: a suíte unitária precisa passar.
- **coverage** (JDK 21) — gera e publica o relatório JaCoCo como artefato.

## Estudo de caso: bugs encontrados pelos testes

Os testes não são decorativos — durante sua escrita, encontraram **3 bugs reais**
que estavam invisíveis na navegação manual:

| # | Bug | Severidade | Como foi descoberto |
|---|---|---|---|
| [#3](https://github.com/scheffler5/Spring_Boot_Clinica/issues/3) | Cadastro de paciente falhava com HTTP 500 (coluna `email` NOT NULL no schema vs. entidade nullable) | Alta | `PatientOnboardingE2ETest` |
| [#4](https://github.com/scheffler5/Spring_Boot_Clinica/issues/4) | `AgendamentoController` sem `@PreAuthorize`: paciente listava agendamentos de todos (vazamento de PII) e alterava status de qualquer um | **Alta (segurança)** | `AccessControlE2ETest` |
| [#6](https://github.com/scheffler5/Spring_Boot_Clinica/issues/6) | `POST /medico/disponibilidade` falhava com 500 (coluna órfã `duracao_consulta_minutos` NOT NULL) | Alta | `MedicoOnboardingE2ETest` |

O #4 foi **corrigido** e travado por teste de regressão. Os #3 e #6 são o mesmo
padrão — *drift* de schema com `ddl-auto=update` — o que motiva a recomendação de
adotar migrações de banco (Flyway/Liquibase).
