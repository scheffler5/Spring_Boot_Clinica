const API   = "";
const token = localStorage.getItem("token");
const role  = localStorage.getItem("role");

if (!token || role !== "PACIENTE") {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    window.location.href = "patient-login.html";
}

if (localStorage.getItem("perfilCompleto") !== "true") {
    window.location.href = "patient-profile-complete.html";
}

const msgBox = document.getElementById("message");

function showMsg(text, type) {
    msgBox.textContent = text;
    msgBox.className = "message " + type + " card";
    msgBox.classList.remove("hidden");
}

async function apiFetch(path, options = {}) {
    const res = await fetch(API + path, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            Authorization: "Bearer " + token,
            ...(options.headers || {})
        }
    });

    if (res.status === 401 || res.status === 403) {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        window.location.href = "patient-login.html";
        throw new Error("Sessão expirada");
    }

    return res;
}

function formatDate(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    return d.toLocaleDateString("pt-BR") + " " + d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}

function formatDateOnly(iso) {
    if (!iso) return "—";
    const [y, m, d] = iso.split("-");
    return `${d}/${m}/${y}`;
}

function currency(value) {
    return Number(value).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}


document.getElementById("btn-logout").addEventListener("click", () => {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    window.location.href = "patient-login.html";
});


async function loadProfile() {
    const res = await apiFetch("/patient/me");
    if (!res.ok) { showMsg("Erro ao carregar perfil.", "error"); return; }

    const p = await res.json();

    const nome = p.nome || "Paciente";
    document.getElementById("topbar-name").textContent = nome;
    document.getElementById("welcome-name").textContent = "Olá, " + nome.split(" ")[0] + "!";

    document.getElementById("profile-grid").innerHTML = `
        <div class="profile-item">
            <label>Nome completo</label>
            <span>${p.nome}</span>
        </div>
        <div class="profile-item">
            <label>CPF</label>
            <span>${formatCpf(p.cpf)}</span>
        </div>
        <div class="profile-item">
            <label>Data de nascimento</label>
            <span>${formatDateOnly(p.dataNascimento)}</span>
        </div>
        <div class="profile-item">
            <label>Status</label>
            <span class="badge ${p.ativo ? 'active' : 'inactive'}">${p.ativo ? 'Ativo' : 'Inativo'}</span>
        </div>
    `;
}

function formatCpf(cpf) {
    if (!cpf || cpf.length !== 11) return cpf;
    return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4");
}


async function loadAppointments() {
    const res = await apiFetch("/patient/appointments");
    if (!res.ok) { showMsg("Erro ao carregar agendamentos.", "error"); return; }

    const list = await res.json();
    const container = document.getElementById("appointments-container");

    if (list.length === 0) {
        container.innerHTML = '<p class="empty-state">Nenhum agendamento encontrado.</p>';
        return;
    }

    const STATUS_LABELS = {
        AGENDADO: "Agendado",
        CONFIRMADO: "Confirmado",
        CANCELADO: "Cancelado",
        REALIZADO: "Realizado",
    };
    const statusLabel = a => STATUS_LABELS[a.status] || a.status || "—";

    const now = new Date();
    const futuros  = list.filter(a => new Date(a.dataHora) >= now);
    const passados = list.filter(a => new Date(a.dataHora) <  now);

    let html = "";

    if (futuros.length > 0) {
        html += `<p style="font-size:12px;color:var(--muted);margin-bottom:8px;font-weight:600">PRÓXIMAS CONSULTAS</p>`;
        html += futuros.map(a => `
            <div style="border-left:3px solid var(--primary);padding:8px 12px;margin-bottom:8px;background:var(--primary-light);border-radius:0 6px 6px 0">
                <div style="font-weight:600;font-size:14px">${formatDate(a.dataHora)}</div>
                <div style="font-size:12px;color:var(--muted)">Médico: ${a.nomeMedico || "—"} · Status: ${statusLabel(a)}</div>
            </div>`).join("");
    }

    if (passados.length > 0) {
        html += `<p style="font-size:12px;color:var(--muted);margin:12px 0 8px;font-weight:600">HISTÓRICO DE CONSULTAS</p>`;
        html += `<table><thead><tr><th>Data</th><th>Médico</th><th>Status</th></tr></thead><tbody>`;
        html += passados.map(a => `
            <tr>
                <td>${formatDate(a.dataHora)}</td>
                <td>${a.nomeMedico || "—"}</td>
                <td>${statusLabel(a)}</td>
            </tr>`).join("");
        html += `</tbody></table>`;
    }

    container.innerHTML = html;
}


async function loadProntuarios() {
    const res = await apiFetch("/patient/prontuarios");
    if (!res.ok) { showMsg("Erro ao carregar histórico médico.", "error"); return; }

    const list = await res.json();
    const container = document.getElementById("prontuarios-container");

    if (list.length === 0) {
        container.innerHTML = '<p class="empty-state">Nenhum atendimento registrado.</p>';
        return;
    }

    const rows = list.map(p => `
        <div style="border:1px solid #eceff1;border-radius:8px;padding:12px;margin-bottom:10px">
            <div style="display:flex;justify-content:space-between;align-items:flex-start">
                <div>
                    <div style="font-weight:600;font-size:14px">${p.descricaoProcedimento}</div>
                    <div style="font-size:12px;color:var(--muted);margin-top:2px">
                        Médico: ${p.nomeMedico} · Convênio: ${p.nomeConvenio}
                    </div>
                    ${p.observacoes ? `<div style="font-size:12px;margin-top:6px;color:#546e7a">${p.observacoes}</div>` : ""}
                </div>
                <div style="text-align:right;white-space:nowrap">
                    <div style="font-weight:600;color:var(--primary-dark)">${currency(p.valorCalculado)}</div>
                    <div style="font-size:11px;color:var(--muted)">${formatDate(p.dataAtendimento)}</div>
                </div>
            </div>
        </div>`).join("");

    container.innerHTML = rows;
}


loadProfile();
loadAppointments();
loadProntuarios();
