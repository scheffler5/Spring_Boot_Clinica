# PRD — Enriquecimento do Agendamento (Médico + Status)

| | |
|---|---|
| **Autor** | Lucas Scheffer |
| **Data** | 2026-06-22 |
| **Status** | Proposto |
| **Escopo desta leva** | Vincular médico à consulta + ciclo de status |

---

## 1. Contexto e problema

Hoje o `Agendamento` armazena apenas `paciente` e `dataHora`. Isso gera duas limitações:

1. **Não há médico vinculado** à consulta — impossível montar "agenda do médico", relatórios ou gerar prontuário a partir da consulta.
2. **A checagem de conflito é global** (`existsByDataHora`): a clínica inteira só pode ter **uma** consulta por horário, o que é irreal — médicos diferentes deveriam poder atender no mesmo horário.

Além disso, não existe **status** da consulta, então não é possível distinguir um agendamento futuro de um cancelado ou já realizado.

## 2. Objetivo

Permitir que cada consulta seja vinculada a um **médico** e tenha um **ciclo de vida** (status), corrigindo a regra de conflito para ser **por médico**.

### Fora de escopo (próximas levas)
- Vincular `Procedimento` e `Local` ao agendamento.
- E-mail de confirmação/lembrete.
- Geração de `Prontuario` a partir da consulta realizada.

## 3. Decisões de design

| Decisão | Escolha |
|---|---|
| Campos novos nesta leva | `medico`, `status` |
| Regra de conflito | **Por médico** — bloqueia só se o mesmo médico já tem consulta no horário |
| Quem pode ser "médico" | `Usuario` com role `MEDIC` ou `ADMIN` |
| Status inicial no create | `AGENDADO` |

## 4. Requisitos funcionais

- **RF1** — Ao criar um agendamento, informar o `medicoId`; o sistema valida que o usuário existe, está ativo e tem role `MEDIC`/`ADMIN`.
- **RF2** — O sistema rejeita o agendamento se o **mesmo médico** já tiver consulta no mesmo `dataHora`.
- **RF3** — Todo agendamento nasce com status `AGENDADO`.
- **RF4** — É possível transicionar o status para `CONFIRMADO`, `CANCELADO` ou `REALIZADO` via endpoint dedicado.
- **RF5** — As respostas de agendamento passam a incluir `nomeMedico` e `status`.
- **RF6** — A tela da recepção lista os médicos disponíveis num dropdown ao agendar.
- **RF7** — O portal do paciente exibe médico e status em cada consulta.

## 5. Modelo de dados

### Enum novo: `StatusAgendamento`
```
AGENDADO, CONFIRMADO, CANCELADO, REALIZADO
```

### `Agendamento` (tabela `tb_appointments`) — campos adicionados
| Campo | Tipo | Observação |
|---|---|---|
| `medico` | `Usuario` (`@ManyToOne`, FK `usuario_id`, `NOT NULL`) | role MEDIC/ADMIN |
| `status` | `StatusAgendamento` (`@Enumerated(STRING)`, `NOT NULL`) | default `AGENDADO` |

## 6. API

### `POST /appointments` — payload atualizado
```json
{
  "pacienteId": "uuid",
  "medicoId": "uuid",
  "dataHora": "2026-07-01T14:30:00"
}
```

### `PATCH /appointments/{id}/status` — novo
```json
{ "status": "CONFIRMADO" }
```

### `GET /appointments` / `GET /appointments/{id}` / `GET /appointments/paciente/{id}` — resposta atualizada
```json
{
  "id": "uuid",
  "pacienteId": "uuid",
  "nomePaciente": "Maria Silva",
  "medicoId": "uuid",
  "nomeMedico": "Dr. João",
  "dataHora": "2026-07-01T14:30:00",
  "status": "AGENDADO",
  "createdAt": "2026-06-22T10:00:00"
}
```

### `GET /users/medicos` — novo
Lista usuários com role MEDIC/ADMIN para popular o dropdown da recepção.

## 7. Mudanças por arquivo

### Backend
| # | Arquivo | Mudança |
|---|---|---|
| 1 | `model/agendamento/StatusAgendamento.java` *(novo)* | Enum com os 4 estados |
| 2 | `model/agendamento/Agendamento.java` | `+ medico` (`@ManyToOne`), `+ status`; ajustar construtor |
| 3 | `dto/agendamento/AppointmentRequestDTO.java` | `+ medicoId` (`@NotNull`) |
| 4 | `dto/agendamento/AppointmentResponseDTO.java` | `+ medicoId`, `+ nomeMedico`, `+ status` |
| 5 | `dto/agendamento/AppointmentStatusDTO.java` *(novo)* | record com `status` (`@NotNull`) |
| 6 | `service/Agendamento/AgendamentoService.java` | validar médico; conflito por médico; `updateStatus()` |
| 7 | `repository/AgendamentoRepository.java` | trocar `existsByDataHora` → `existsByMedicoIdAndDataHora`; `+ findAllByMedicoId` |
| 8 | `controller/agendamento/AgendamentoController.java` | `+ PATCH /{id}/status` |
| 9 | `repository/UsuarioRepository.java` | `+ findByRoleIn(...)` (ou equivalente) para listar médicos |
| 10 | `controller/...` (users) | `+ GET /users/medicos` |

### Frontend
| # | Arquivo | Mudança |
|---|---|---|
| 11 | `static/dashboard.html` | `<select>` de médico no form de agendamento |
| 12 | `static/js/dashboard.js` | popular select de médicos; enviar `medicoId`; ações de status |
| 13 | `static/js/patient-dashboard.js` | exibir médico + status nos cards |

## 8. Impacto no banco

- **Docker** (`DDL_AUTO=create-drop`): schema recriado a cada boot — colunas `NOT NULL` entram sem migração.
- **Local** (`DDL_AUTO=update`): adicionar colunas `NOT NULL` em tabela com dados existentes falha. **Ação:** dropar `tb_appointments` uma vez antes de subir, ou subir as colunas como nullable.

## 9. Critérios de aceite

- [ ] Criar agendamento sem `medicoId` retorna 400.
- [ ] Criar agendamento com médico inexistente/sem role retorna erro de negócio.
- [ ] Dois médicos diferentes podem ter consulta no mesmo horário.
- [ ] Mesmo médico com dois agendamentos no mesmo horário é bloqueado.
- [ ] Novo agendamento retorna `status = AGENDADO`.
- [ ] `PATCH /appointments/{id}/status` altera o status e reflete no `GET`.
- [ ] Dropdown da recepção lista os médicos.
- [ ] Portal do paciente mostra médico e status.

## 10. Riscos

| Risco | Mitigação |
|---|---|
| Perda de dados ao recriar `tb_appointments` em dev | Ambiente de desenvolvimento, sem dados de produção |
| `ADMIN` aparecer como "médico" no dropdown | Aceitável (ADMIN herda MEDIC); reavaliar se incomodar |
