const API   = "";
const token = localStorage.getItem("token");
const role  = localStorage.getItem("role");

if (!token || role !== "PACIENTE") { window.location.href = "patient-login.html"; }
if (localStorage.getItem("perfilCompleto") !== "true") { window.location.href = "patient-profile-complete.html"; }

const msgBox = document.getElementById("message");
function showMsg(text, type, duration = 5000) {
    msgBox.textContent = text;
    msgBox.className = "message " + type;
    msgBox.classList.remove("hidden");
    setTimeout(() => msgBox.classList.add("hidden"), duration);
}

async function apiFetch(path, options = {}) {
    const res = await fetch(API + path, {
        ...options,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + token, ...(options.headers || {}) }
    });
    if (res.status === 401) { localStorage.clear(); window.location.href = "patient-login.html"; throw new Error(); }
    return res;
}

function currency(v) { return Number(v || 0).toLocaleString("pt-BR", { style: "currency", currency: "BRL" }); }

document.getElementById("btn-logout").addEventListener("click", () => {
    localStorage.clear(); window.location.href = "patient-login.html";
});

let allMedicos   = [];
let selectedMedico = null;
let selectedSlot   = null;

async function carregarEspecialidades() {
    const data = await fetch(`${API}/medico/especialidades`).then(r => r.json());
    const sel  = document.getElementById("filtro-especialidade");
    data.forEach(({ value, label }) => {
        const o = document.createElement("option"); o.value = value; o.textContent = label; sel.appendChild(o);
    });
}

async function carregarMedicos() {
    const esp    = document.getElementById("filtro-especialidade").value;
    const cidade = document.getElementById("filtro-cidade").value.trim();
    const params = new URLSearchParams();
    if (esp)    params.set("especialidade", esp);
    if (cidade) params.set("cidade", cidade);

    const res  = await apiFetch(`/patient/medicos?${params}`);
    const body = await res.json();
    allMedicos = Array.isArray(body) ? body : [];
    renderMedicos();
}

function avatarHtml(m) {
    if (m.fotoUrl) {
        return `<div class="doctor-avatar"><img src="${m.fotoUrl}" alt="${m.nome}" onerror="this.parentElement.textContent='🩺'"></div>`;
    }
    return `<div class="doctor-avatar">🩺</div>`;
}

function renderMedicos() {
    const grid = document.getElementById("medicos-grid");
    document.getElementById("mp-count").textContent =
        allMedicos.length === 0 ? "" : `${allMedicos.length} médico(s) encontrado(s)`;

    if (allMedicos.length === 0) {
        grid.innerHTML = '<p class="empty-state" style="grid-column:1/-1">Nenhum médico disponível com esses filtros.</p>';
        return;
    }

    grid.innerHTML = allMedicos.map(m => `
        <div class="doctor-card" onclick="verMedico('${m.id}')" style="cursor:pointer">
            <div class="doctor-card-header">
                ${avatarHtml(m)}
                <div class="doctor-info">
                    <h3>${m.nome || m.login}</h3>
                    <span class="specialty-badge">${m.especialidadeDescricao || ''}</span>
                    ${m.cidade ? `<div class="doctor-city">📍 ${m.cidade}</div>` : ''}
                </div>
            </div>
            <div class="doctor-card-footer">
                <div>
                    <div class="doctor-price">${currency(m.valorConsulta)}</div>
                    <div class="doctor-dur">${m.duracaoConsultaMinutos || 60} min</div>
                </div>
                <button class="btn-book" onclick="event.stopPropagation();verMedico('${m.id}')">Ver perfil →</button>
            </div>
        </div>
    `).join("");
}

window.verMedico = function(id) {
    window.location.href = `doctor-view.html?id=${id}`;
};

function renderBookingPanel(m) {
    const hoje = new Date().toISOString().split("T")[0];
    return `
        <div class="booking-panel">
            <h3>📅 Agendar com ${m.nome}</h3>
            <p>${m.especialidadeDescricao} · ${currency(m.valorConsulta)} · ${m.duracaoConsultaMinutos || 60} min</p>
            <div class="booking-date-row">
                <label style="font-size:14px;color:var(--muted)">Escolha a data:</label>
                <input id="booking-date" type="date" min="${hoje}" onchange="buscarSlots()">
            </div>
            <div id="slots-area"></div>
            <button class="btn-confirm" id="btn-confirmar" disabled onclick="abrirConfirmacao()">
                Confirmar agendamento
            </button>
        </div>
    `;
}

window.selecionarMedico = function(id) {
    const anterior = selectedMedico?.id;
    selectedMedico = anterior === id ? null : (allMedicos.find(m => m.id === id) || null);
    selectedSlot   = null;
    renderMedicos();
    if (selectedMedico) {
        const card = document.getElementById("card-" + id);
        if (card) setTimeout(() => card.scrollIntoView({ behavior: "smooth", block: "nearest" }), 50);
    }
};

window.buscarSlots = async function() {
    const data = document.getElementById("booking-date")?.value;
    const area = document.getElementById("slots-area");
    if (!data || !selectedMedico || !area) return;

    area.innerHTML = `<p class="no-slots">Buscando horários...</p>`;
    selectedSlot = null;
    const btn = document.getElementById("btn-confirmar");
    if (btn) btn.disabled = true;

    const res   = await apiFetch(`/patient/medicos/${selectedMedico.id}/horarios?data=${data}`);
    const slots = await res.json();

    if (!slots.length) {
        area.innerHTML = `<p class="no-slots">Nenhum horário disponível nesta data. Tente outra data.</p>`;
        return;
    }

    area.innerHTML = `
        <div class="slots-label">Horários disponíveis — escolha apenas um</div>
        <div class="slots-grid">
            ${slots.map(s => {
                const hora = new Date(s).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
                return `<button class="slot-pill" onclick="selecionarSlot('${s}', this)">${hora}</button>`;
            }).join("")}
        </div>`;
};

window.selecionarSlot = function(slot, el) {
    document.querySelectorAll(".slot-pill").forEach(p => p.classList.remove("selected"));
    el.classList.add("selected");
    selectedSlot = slot;
    const btn = document.getElementById("btn-confirmar");
    if (btn) btn.disabled = false;
};

window.abrirConfirmacao = function() {
    if (!selectedMedico || !selectedSlot) return;
    const dt = new Date(selectedSlot);
    const quando = dt.toLocaleDateString("pt-BR", { weekday: "long", day: "numeric", month: "long" })
                 + " às " + dt.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
    document.getElementById("confirm-text").textContent =
        `Confirmar consulta com ${selectedMedico.nome} em ${quando}?`;
    document.getElementById("confirm-overlay").classList.remove("hidden");
};

document.getElementById("confirm-cancel").addEventListener("click", () => {
    document.getElementById("confirm-overlay").classList.add("hidden");
});

document.getElementById("confirm-ok").addEventListener("click", async () => {
    document.getElementById("confirm-overlay").classList.add("hidden");

    const res  = await apiFetch("/patient/agendamentos", {
        method: "POST",
        body: JSON.stringify({ medicoId: selectedMedico.id, dataHora: selectedSlot })
    });

    if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        showMsg(err.message || "Erro ao agendar.", "error");
        return;
    }

    showMsg("✅ Consulta agendada com sucesso!", "success", 6000);
    selectedMedico = null;
    selectedSlot   = null;
    renderMedicos();
});

let debounce;
document.getElementById("filtro-especialidade").addEventListener("change", carregarMedicos);
document.getElementById("filtro-cidade").addEventListener("input", () => {
    clearTimeout(debounce);
    debounce = setTimeout(carregarMedicos, 500);
});

async function init() {
    try {
        const res = await apiFetch("/patient/me");
        if (res.ok) {
            const p = await res.json();
            document.getElementById("topbar-name").textContent = "Olá, " + (p.nome || "Paciente").split(" ")[0];
            localStorage.setItem("perfilCompleto", "true");
        }
    } catch {}
    await carregarEspecialidades();
    await carregarMedicos();
    return true;
}

init().then(() => {
    const TOUR_MARKETPLACE = [
        {
            icon: "🏥",
            title: "Encontre seu médico",
            text:  "Aqui você encontra todos os médicos disponíveis para consulta. Vamos ver como usar o marketplace!"
        },
        {
            icon: "🔍",
            title: "Filtros de busca",
            target: ".mp-filters",
            text:  "Filtre por especialidade médica ou digite uma cidade para encontrar profissionais próximos a você."
        },
        {
            icon: "👨‍⚕️",
            title: "Cards de médicos",
            target: ".mp-grid",
            text:  "Cada card mostra foto, especialidade, cidade e valor da consulta. Clique em \"Ver perfil →\" para acessar o perfil completo e agendar."
        },
        {
            icon: "✅",
            title: "Pronto para agendar!",
            text:  "Escolha um médico, veja os horários disponíveis e confirme sua consulta em poucos segundos."
        }
    ];
    setTimeout(() => {
        const tour = new TourGuide("marketplace-v1", TOUR_MARKETPLACE);
        tour.start();
    }, 800);
});
