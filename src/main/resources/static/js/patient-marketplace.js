const API   = "";
const token = localStorage.getItem("token");
const role  = localStorage.getItem("role");

if (!token || role !== "PACIENTE") {
    localStorage.clear();
    window.location.href = "patient-login.html";
}

// Sem perfil completo não dá para agendar.
if (localStorage.getItem("perfilCompleto") !== "true") {
    window.location.href = "patient-profile-complete.html";
}

const msgBox = document.getElementById("message");

function showMsg(text, type) {
    msgBox.textContent = text;
    msgBox.className = "message " + type;
    msgBox.classList.remove("hidden");
    if (type === "success") setTimeout(() => msgBox.classList.add("hidden"), 5000);
}

async function apiFetch(path, options = {}) {
    const res = await fetch(API + path, {
        ...options,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + token, ...(options.headers || {}) }
    });
    if (res.status === 401 || res.status === 403) {
        localStorage.clear();
        window.location.href = "patient-login.html";
        throw new Error("Sessão expirada");
    }
    return res;
}

document.getElementById("btn-logout").addEventListener("click", () => {
    localStorage.clear();
    window.location.href = "patient-login.html";
});

function currency(v) {
    return v == null ? "—" : Number(v).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function formatHora(iso) {
    return new Date(iso).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}

let selectedMedico = null;

async function loadEspecialidades() {
    try {
        const res  = await apiFetch("/patient/especialidades");
        const list = await res.json();
        const sel  = document.getElementById("filtro-especialidade");
        sel.innerHTML = '<option value="">Todas as especialidades</option>' +
            list.map(e => `<option value="${e.nome}">${e.descricao}</option>`).join("");
    } catch (_) { /* sessão expirada já tratada */ }
}

async function loadMedicos(especialidade = "") {
    const grid = document.getElementById("medicos-grid");
    grid.innerHTML = '<p class="empty-state">Carregando médicos...</p>';

    const query = especialidade ? `?especialidade=${encodeURIComponent(especialidade)}` : "";
    const res   = await apiFetch(`/patient/medicos${query}`);
    const list  = await res.json();

    if (list.length === 0) {
        grid.innerHTML = '<p class="empty-state">Nenhum médico disponível para esta especialidade.</p>';
        return;
    }

    grid.innerHTML = list.map(m => `
        <div class="card">
            <h2 style="margin-bottom:4px">${m.nome}</h2>
            <span class="badge active">${m.especialidadeDescricao || "—"}</span>
            <p style="font-size:13px;color:var(--muted);margin:10px 0 4px">CRM: ${m.crm || "—"}</p>
            <p style="font-weight:600;color:var(--primary-dark)">${currency(m.valorConsulta)}</p>
            <button class="btn-primary" style="margin-top:12px"
                onclick='selectMedico(${JSON.stringify(m).replace(/'/g, "&#39;")})'>
                Ver horários
            </button>
        </div>`).join("");
}

function selectMedico(m) {
    selectedMedico = m;
    document.getElementById("booking-medico-nome").textContent = m.nome;
    document.getElementById("booking-medico-info").textContent =
        `${m.especialidadeDescricao} · CRM ${m.crm || "—"} · ${currency(m.valorConsulta)}`;

    const dataInput = document.getElementById("booking-data");
    const hoje = new Date().toISOString().split("T")[0];
    dataInput.min = hoje;
    dataInput.value = "";

    document.getElementById("slots-container").innerHTML =
        '<p class="empty-state">Selecione uma data para ver os horários.</p>';

    document.getElementById("booking-panel").classList.remove("hidden");
    document.getElementById("booking-panel").scrollIntoView({ behavior: "smooth", block: "start" });
}

document.getElementById("filtro-especialidade").addEventListener("change", e => {
    document.getElementById("booking-panel").classList.add("hidden");
    selectedMedico = null;
    loadMedicos(e.target.value);
});

document.getElementById("booking-data").addEventListener("change", loadSlots);

async function loadSlots() {
    if (!selectedMedico) return;
    const data = document.getElementById("booking-data").value;
    const container = document.getElementById("slots-container");
    if (!data) { container.innerHTML = '<p class="empty-state">Selecione uma data.</p>'; return; }

    container.innerHTML = '<p class="empty-state">Carregando horários...</p>';
    const res   = await apiFetch(`/patient/medicos/${selectedMedico.id}/horarios?data=${data}`);
    const slots = await res.json();

    if (slots.length === 0) {
        container.innerHTML = '<p class="empty-state">Nenhum horário disponível neste dia.</p>';
        return;
    }

    container.innerHTML = '<div style="display:flex;flex-wrap:wrap;gap:8px">' +
        slots.map(s => `<button class="slot-btn" onclick="book('${s}')"
            style="padding:8px 14px;border:1px solid var(--primary);background:#fff;color:var(--primary-dark);border-radius:6px;cursor:pointer">
            ${formatHora(s)}</button>`).join("") +
        '</div>';
}

async function book(dataHora) {
    if (!selectedMedico) return;
    if (!confirm(`Confirmar consulta com ${selectedMedico.nome} em ${new Date(dataHora).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" })}?`)) return;

    const res = await apiFetch("/patient/agendamentos", {
        method: "POST",
        body: JSON.stringify({ medicoId: selectedMedico.id, dataHora })
    });
    const body = await res.json().catch(() => null);

    if (!res.ok) {
        showMsg(body?.message || "Não foi possível agendar.", "error");
        return;
    }

    showMsg("Consulta confirmada! Veja em 'Minha Área'.", "success");
    loadSlots(); // recarrega para remover o horário recém-agendado
}

loadEspecialidades();
loadMedicos();
