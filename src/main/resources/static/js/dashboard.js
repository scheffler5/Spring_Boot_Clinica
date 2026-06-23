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
        extendedProps: { status: a.status, pacienteId: a.pacienteId, medicoId: a.medicoId }
    }));

    const el = document.getElementById("calendar");
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

function abrirModal(event) {
    const props = event.extendedProps;
    document.getElementById("modal-titulo").textContent = event.title;
    document.getElementById("modal-corpo").innerHTML = `
        <div class="modal-field"><label>Data / Hora</label><span>${fmt(event.start)}</span></div>
        <div class="modal-field"><label>Status</label><span class="status-badge status-${props.status}">${props.status}</span></div>
        <div class="modal-field"><label>ID do Paciente</label><span>${props.pacienteId}</span></div>
    `;
    document.getElementById("modal-paciente").classList.remove("hidden");
}

document.getElementById("modal-fechar").addEventListener("click", () => {
    document.getElementById("modal-paciente").classList.add("hidden");
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

carregarPerfil();
carregarEstatisticas();
carregarCalendario();
carregarDisponibilidade();
