const API   = "";
const token = localStorage.getItem("token");
const role  = localStorage.getItem("role");

if (!token || (role !== "MEDIC" && role !== "ADMIN")) {
    window.location.href = "index.html";
}

if (localStorage.getItem("perfilCompleto") !== "true") {
    window.location.href = "doctor-profile-complete.html";
}

async function apiFetch(path, options = {}) {
    const res = await fetch(API + path, {
        ...options,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + token, ...(options.headers || {}) }
    });
    if (res.status === 401 || res.status === 403) { window.location.href = "index.html"; throw new Error("Sessão expirada"); }
    return res;
}

function currency(v) { return Number(v).toLocaleString("pt-BR", { style: "currency", currency: "BRL" }); }
function fmt(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    return d.toLocaleDateString("pt-BR") + " " + d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}

const msgBox = document.getElementById("message");
function showMsg(text, type) { msgBox.textContent = text; msgBox.className = "message " + type; setTimeout(() => msgBox.className = "message hidden", 5000); }

document.getElementById("btn-logout").addEventListener("click", () => {
    ["token", "role", "perfilCompleto"].forEach(k => localStorage.removeItem(k));
    window.location.href = "index.html";
});

document.querySelectorAll(".nav-tab").forEach(tab => {
    tab.addEventListener("click", () => {
        document.querySelectorAll(".nav-tab").forEach(t => t.classList.remove("active"));
        document.querySelectorAll(".dashboard-section").forEach(s => s.classList.add("hidden"));
        tab.classList.add("active");
        document.getElementById("section-" + tab.dataset.section).classList.remove("hidden");
    });
});

async function carregarPerfil() {
    const res  = await apiFetch("/medico/perfil");
    const data = await res.json();
    document.getElementById("topbar-nome").textContent = data.nome || "";
    const dur  = data.duracaoConsultaMinutos || 60;
    const info = document.getElementById("duracao-info");
    if (info) info.textContent = `Duração padrão das consultas: ${dur} minutos (configurada no seu perfil)`;
}

async function carregarEstatisticas(mes) {
    const query = mes ? `?mes=${mes}` : "";
    const res   = await apiFetch(`/medico/estatisticas${query}`);
    const data  = await res.json();
    document.getElementById("stat-atendidos").textContent = data.totalAtendidosMes;
    document.getElementById("stat-agendados").textContent = data.totalAgendadosMes;
    document.getElementById("stat-receita").textContent   = currency(data.receitaRealizadaMes);
    document.getElementById("stat-projecao").textContent  = currency(data.receitaProjetadaMes);
    return data;
}

let calendar;

async function carregarCalendario() {
    const res    = await apiFetch("/medico/agenda");
    const agenda = await res.json();

    const statusColor = {
        AGENDADO:   "#1976d2",
        CONFIRMADO: "#2e7d32",
        CANCELADO:  "#c62828",
        REALIZADO:  "#6a1b9a"
    };

    const events = agenda.map(a => ({
        id:    a.id,
        title: a.nomePaciente,
        start: a.dataHora,
        color: statusColor[a.status] || "#607d8b",
        extendedProps: { agendamentoId: a.id, status: a.status, pacienteId: a.pacienteId, medicoId: a.medicoId }
    }));

    const el = document.getElementById("calendar");
    if (calendar) { calendar.destroy(); }
    calendar = new window.FullCalendar.Calendar(el, {
        initialView:  "dayGridMonth",
        locale:       "pt-br",
        height:       "auto",
        headerToolbar: {
            left:   "prev,next today",
            center: "title",
            right:  "dayGridMonth,timeGridWeek,listWeek"
        },
        events,
        eventClick: (info) => abrirModal(info.event)
    });
    calendar.render();
}

let modalAgendamentoId = null;

function confirmarCancelamento(id, onConfirm) {
    const overlay = document.createElement("div");
    overlay.style.cssText = "position:fixed;inset:0;background:rgba(0,0,0,.45);z-index:3000;display:flex;align-items:center;justify-content:center";

    overlay.innerHTML = `
        <div style="background:#fff;border-radius:14px;padding:28px 24px;width:300px;text-align:center;box-shadow:0 8px 32px rgba(0,0,0,.25)">
            <p style="font-size:28px;margin-bottom:8px">⚠️</p>
            <p style="font-size:16px;font-weight:600;margin-bottom:20px">Cancelar esta consulta?</p>
            <div style="display:flex;gap:10px">
                <button id="_popup_nao" style="flex:1;padding:11px;border:1px solid #ddd;border-radius:8px;background:#f5f5f5;cursor:pointer;font-size:14px">Não</button>
                <button id="_popup_sim" style="flex:1;padding:11px;border:none;border-radius:8px;background:#c62828;color:#fff;cursor:pointer;font-size:14px;font-weight:600">Sim, cancelar</button>
            </div>
        </div>`;

    document.body.appendChild(overlay);

    overlay.querySelector("#_popup_nao").onclick = () => overlay.remove();
    overlay.querySelector("#_popup_sim").onclick = async () => {
        overlay.remove();
        await onConfirm(id);
    };
}

async function executarCancelamento(id) {
    const res = await apiFetch(`/appointments/${id}`, { method: "DELETE" });
    if (res.ok) {
        document.getElementById("modal-consulta").classList.add("hidden");
        showMsg("Consulta cancelada.", "success");
        carregarCalendario();
        carregarEstatisticas();
    } else {
        const err = await res.json().catch(() => ({}));
        showMsg(err.message || "Não foi possível cancelar.", "error");
    }
}

function abrirModal(event) {
    const props      = event.extendedProps;
    const cancelavel = props.status === "AGENDADO" || props.status === "CONFIRMADO";

    modalAgendamentoId = props.agendamentoId;

    document.getElementById("modal-titulo").textContent = event.title;
    document.getElementById("modal-corpo").innerHTML = `
        <div class="modal-field"><label>Data / Hora</label><span>${fmt(event.start)}</span></div>
        <div class="modal-field">
            <label>Status</label>
            <span class="status-badge status-${props.status}">${props.status}</span>
        </div>`;

    document.getElementById("modal-btn-cancelar").classList.toggle("hidden", !cancelavel);
    document.getElementById("modal-consulta").classList.remove("hidden");
}

document.getElementById("modal-fechar").addEventListener("click", () => {
    document.getElementById("modal-consulta").classList.add("hidden");
});

document.getElementById("modal-btn-cancelar").addEventListener("click", () => {
    confirmarCancelamento(modalAgendamentoId, executarCancelamento);
});

async function carregarDisponibilidade() {
    const res  = await apiFetch("/medico/disponibilidade");
    const data = await res.json();
    const el   = document.getElementById("disponibilidade-list");

    if (data.length === 0) {
        el.innerHTML = '<p style="color:var(--muted);font-size:14px">Nenhum horário cadastrado ainda.</p>';
        return;
    }

    el.innerHTML = data.map(d => `
        <div class="disponibilidade-item">
            <span class="dia">${d.diaSemanaLabel}</span>
            <span class="horario">${d.horaInicio} — ${d.horaFim} · ${d.duracaoConsultaMinutos} min</span>
            <button class="btn-remover" onclick="removerDisponibilidade('${d.id}')">Remover</button>
        </div>
    `).join("");
}

window.removerDisponibilidade = async (id) => {
    if (!confirm("Remover este horário?")) return;
    const res = await apiFetch(`/medico/disponibilidade/${id}`, { method: "DELETE" });
    if (res.ok) { showMsg("Horário removido.", "success"); carregarDisponibilidade(); }
    else        { showMsg("Erro ao remover.", "error"); }
};

document.getElementById("form-disponibilidade").addEventListener("submit", async e => {
    e.preventDefault();
    const body = {
        diaSemana:  document.getElementById("dia-semana").value,
        horaInicio: document.getElementById("hora-inicio").value,
        horaFim:    document.getElementById("hora-fim").value
    };
    const res = await apiFetch("/medico/disponibilidade", { method: "POST", body: JSON.stringify(body) });
    const data = await res.json().catch(() => ({}));
    if (res.ok) { showMsg("Horário adicionado!", "success"); e.target.reset(); carregarDisponibilidade(); }
    else        { showMsg(data.message || "Erro ao adicionar horário.", "error"); }
});

document.getElementById("btn-atualizar-stats").addEventListener("click", async () => {
    const mes  = document.getElementById("mes-selecionado").value;
    const data = await carregarEstatisticas(mes || undefined);
    document.getElementById("stats-detalhado").innerHTML = `
        <div class="stat-card"><div class="stat-value">${data.totalAtendidosMes}</div><div class="stat-label">Atendidos</div></div>
        <div class="stat-card"><div class="stat-value">${data.totalAgendadosMes}</div><div class="stat-label">Agendados</div></div>
        <div class="stat-card green"><div class="stat-value">${currency(data.receitaRealizadaMes)}</div><div class="stat-label">Receita realizada</div></div>
        <div class="stat-card blue"><div class="stat-value">${currency(data.receitaProjetadaMes)}</div><div class="stat-label">Projeção</div></div>
    `;
});

async function carregarEspecialidadesPerfil() {
    const data = await fetch(`${API}/medico/especialidades`).then(r => r.json());
    const sel  = document.getElementById("s-especialidade");
    data.forEach(({ value, label }) => {
        const o = document.createElement("option");
        o.value = value; o.textContent = label;
        sel.appendChild(o);
    });
}

async function carregarDadosPerfil() {
    const res  = await apiFetch("/medico/perfil");
    if (!res.ok) return;
    const data = await res.json();

    document.getElementById("s-nome").value              = data.nome              || "";
    document.getElementById("s-crm").value               = data.crm               || "";
    document.getElementById("s-cidade").value            = data.cidade            || "";
    document.getElementById("s-especialidade").value     = data.especialidade     || "";
    document.getElementById("s-valor").value             = data.valorConsulta     || "";
    document.getElementById("s-duracao").value           = data.duracaoConsultaMinutos || 60;
    document.getElementById("s-universidade").value      = data.universidade      || "";
    document.getElementById("s-ano-formacao").value      = data.anoFormacao       || "";
    document.getElementById("s-descricao").value         = data.descricao         || "";

    const avatar = document.getElementById("foto-avatar");
    if (data.fotoUrl) {
        const src = data.fotoUrl.startsWith("/imagens/")
            ? `${data.fotoUrl}?v=${Date.now()}` : data.fotoUrl;
        avatar.innerHTML = `<img src="${src}" style="width:100%;height:100%;object-fit:cover" onerror="this.parentElement.textContent='🩺'">`;
    }
}

document.getElementById("foto-input").addEventListener("change", async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const msgEl  = document.getElementById("perfil-message");
    const avatar = document.getElementById("foto-avatar");

    // Pré-visualiza antes do upload
    const reader = new FileReader();
    reader.onload = ev => {
        avatar.innerHTML = `<img src="${ev.target.result}" style="width:100%;height:100%;object-fit:cover">`;
    };
    reader.readAsDataURL(file);

    msgEl.textContent = "Enviando foto...";
    msgEl.className   = "message info";
    msgEl.classList.remove("hidden");

    const formData = new FormData();
    formData.append("arquivo", file);

    try {
        const res  = await fetch(`${API}/medico/foto`, {
            method:  "POST",
            headers: { Authorization: "Bearer " + token },
            body:    formData
        });

        const data = await res.json().catch(() => ({}));

        if (res.ok && data.fotoUrl) {
            // URL tipo /imagens/{uuid} — adiciona timestamp só para forçar reload do browser
            avatar.innerHTML    = `<img src="${data.fotoUrl}?v=${Date.now()}" style="width:100%;height:100%;object-fit:cover">`;
            msgEl.textContent   = "✓ Foto atualizada!";
            msgEl.className     = "message success";
        } else {
            msgEl.textContent = data.message || "Erro ao enviar foto.";
            msgEl.className   = "message error";
        }
    } catch {
        msgEl.textContent = "Erro de conexão ao enviar a foto.";
        msgEl.className   = "message error";
    }

    e.target.value = "";
    setTimeout(() => msgEl.classList.add("hidden"), 4000);
});

document.getElementById("form-perfil-settings").addEventListener("submit", async (e) => {
    e.preventDefault();
    const msgEl = document.getElementById("perfil-message");

    const anoVal = document.getElementById("s-ano-formacao").value;
    const body = {
        nome:                  document.getElementById("s-nome").value.trim(),
        crm:                   document.getElementById("s-crm").value.trim(),
        cidade:                document.getElementById("s-cidade").value.trim(),
        especialidade:         document.getElementById("s-especialidade").value,
        valorConsulta:         parseFloat(document.getElementById("s-valor").value) || 0,
        duracaoConsultaMinutos: parseInt(document.getElementById("s-duracao").value),
        universidade:          document.getElementById("s-universidade").value.trim() || null,
        anoFormacao:           anoVal ? parseInt(anoVal) : null,
        descricao:             document.getElementById("s-descricao").value.trim() || null
    };

    if (!body.nome) { msgEl.textContent = "Nome é obrigatório."; msgEl.className = "message error"; return; }
    if (!body.especialidade) { msgEl.textContent = "Selecione uma especialidade."; msgEl.className = "message error"; return; }

    const res  = await apiFetch("/medico/perfil", { method: "PUT", body: JSON.stringify(body) });
    const data = await res.json().catch(() => ({}));

    if (res.ok) {
        msgEl.textContent = "Perfil atualizado com sucesso!";
        msgEl.className = "message success";
        document.getElementById("topbar-nome").textContent = data.nome || "";
    } else {
        msgEl.textContent = data.message || "Erro ao salvar perfil.";
        msgEl.className = "message error";
    }
    setTimeout(() => msgEl.classList.add("hidden"), 4000);
});

function ativarAba(secao) {
    document.querySelectorAll(".nav-tab").forEach(t => t.classList.remove("active"));
    document.querySelectorAll(".dashboard-section").forEach(s => s.classList.add("hidden"));
    const tab = document.querySelector(`.nav-tab[data-section="${secao}"]`);
    if (tab) tab.classList.add("active");
    const sec = document.getElementById(`section-${secao}`);
    if (sec) sec.classList.remove("hidden");
}

const TOUR_MEDICO = [
    {
        icon: "🎉",
        title: "Bem-vindo ao seu painel!",
        text:  "Este é o seu dashboard médico. Vamos fazer um tour rápido pelas principais funcionalidades para você aproveitar ao máximo."
    },
    {
        icon: "📊",
        title: "Estatísticas mensais",
        target: ".stats-bar",
        text:  "Acompanhe em tempo real: pacientes atendidos, agendamentos futuros, receita realizada e projeção para o mês."
    },
    {
        icon: "📅",
        title: "Calendário de consultas",
        target: "#calendar",
        text:  "Visualize todas as suas consultas. Clique em qualquer evento para ver os detalhes do paciente e opções de gerenciamento."
    },
    {
        icon: "🕒",
        title: "Configure sua disponibilidade",
        target: ".nav-tab[data-section='disponibilidade']",
        action: () => ativarAba("disponibilidade"),
        text:  "Defina os dias e horários em que você atende. Os pacientes só poderão agendar nos slots que você liberar aqui."
    },
    {
        icon: "⚙️",
        title: "Seu perfil público",
        target: ".nav-tab[data-section='perfil']",
        action: () => ativarAba("perfil"),
        text:  "Mantenha seu perfil completo: foto, especialidade, universidade de formação e uma apresentação. Um perfil completo atrai mais pacientes!"
    },
    {
        icon: "✅",
        title: "Você está pronto!",
        action: () => ativarAba("agenda"),
        text:  "Explore todas as funcionalidades. Se quiser rever este tour, acesse o seu perfil e clique em \"Ver tour novamente\"."
    }
];

carregarPerfil();
carregarEstatisticas();
carregarCalendario();
carregarDisponibilidade();
carregarEspecialidadesPerfil().then(carregarDadosPerfil);

setTimeout(() => {
    const tour = new TourGuide("medico-v1", TOUR_MEDICO);
    tour.start();
}, 1200);
