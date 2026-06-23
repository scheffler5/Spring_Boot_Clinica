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

    const avatarHtml = p.fotoUrl
        ? `<img src="${p.fotoUrl}" style="width:80px;height:80px;border-radius:50%;object-fit:cover;border:3px solid var(--primary-light)" onerror="this.style.display='none'">`
        : `<div style="width:80px;height:80px;border-radius:50%;background:var(--primary-light);display:flex;align-items:center;justify-content:center;font-size:36px">👤</div>`;

    document.getElementById("profile-grid").innerHTML = `
        <div style="grid-column:1/-1;display:flex;align-items:center;gap:20px;margin-bottom:8px">
            <div id="patient-avatar-wrap" style="position:relative;cursor:pointer" title="Clique para trocar a foto" onclick="document.getElementById('input-foto-paciente').click()">
                ${avatarHtml}
                <div style="position:absolute;bottom:0;right:0;background:var(--primary);color:#fff;border-radius:50%;width:24px;height:24px;display:flex;align-items:center;justify-content:center;font-size:13px;box-shadow:0 1px 4px rgba(0,0,0,.3)">✏️</div>
            </div>
            <div>
                <div style="font-weight:700;font-size:16px">${p.nome}</div>
                <div style="font-size:12px;color:var(--muted);margin-top:2px">Clique na foto para atualizar</div>
            </div>
            <input type="file" id="input-foto-paciente" accept="image/jpeg,image/png,image/webp" style="display:none" onchange="uploadFotoPaciente(this)">
        </div>
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

window.uploadFotoPaciente = async function(input) {
    const file = input.files[0];
    if (!file) return;
    const wrap = document.getElementById("patient-avatar-wrap");
    wrap.style.opacity = "0.5";

    const form = new FormData();
    form.append("arquivo", file);

    try {
        const res = await fetch(`${API}/patient/foto`, {
            method: "POST",
            headers: { Authorization: "Bearer " + token },
            body: form
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            showMsg(err.message || "Erro ao enviar foto.", "error");
            return;
        }
        const data = await res.json();
        const novaUrl = data.fotoUrl;
        const img = wrap.querySelector("img") || wrap.querySelector("div");
        if (novaUrl) {
            wrap.innerHTML = `
                <img src="${novaUrl}" style="width:80px;height:80px;border-radius:50%;object-fit:cover;border:3px solid var(--primary-light)">
                <div style="position:absolute;bottom:0;right:0;background:var(--primary);color:#fff;border-radius:50%;width:24px;height:24px;display:flex;align-items:center;justify-content:center;font-size:13px;box-shadow:0 1px 4px rgba(0,0,0,.3)">✏️</div>
                <input type="file" id="input-foto-paciente" accept="image/jpeg,image/png,image/webp" style="display:none" onchange="uploadFotoPaciente(this)">`;
        }
        showMsg("Foto atualizada com sucesso!", "success");
    } catch {
        showMsg("Erro ao enviar foto.", "error");
    } finally {
        wrap.style.opacity = "1";
        input.value = "";
    }
};

function formatCpf(cpf) {
    if (!cpf || cpf.length !== 11) return cpf;
    return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4");
}


function avatarMedico(a, size = 44) {
    const s = `width:${size}px;height:${size}px;border-radius:50%;object-fit:cover;flex-shrink:0`;
    if (a.fotoMedicoUrl) {
        return `<img src="${a.fotoMedicoUrl}" style="${s}" onerror="this.outerHTML='<div style=\'${s};background:var(--primary-light);display:flex;align-items:center;justify-content:center;font-size:${size*0.45}px\'>🩺</div>'">`;
    }
    return `<div style="${s};background:var(--primary-light);display:flex;align-items:center;justify-content:center;font-size:${size*0.45}px">🩺</div>`;
}

async function loadAppointments() {
    const res = await apiFetch("/patient/appointments");
    if (!res.ok) { showMsg("Erro ao carregar agendamentos.", "error"); return; }

    const list      = await res.json();
    const container = document.getElementById("appointments-container");

    if (list.length === 0) {
        container.innerHTML = '<p class="empty-state">Nenhum agendamento ainda. <a href="patient-marketplace.html">Agendar consulta →</a></p>';
        return;
    }

    const now = new Date();

    const proximas   = list.filter(a => new Date(a.dataHora) >= now && a.status !== "CANCELADO");
    const realizadas = list.filter(a => new Date(a.dataHora) <  now && a.status === "REALIZADO");
    const canceladas = list.filter(a => a.status === "CANCELADO");

    let html = "";

    // ── Próximas ──────────────────────────────────────────────────────
    if (proximas.length > 0) {
        html += `<p class="appt-section-label">Próximas consultas</p>`;
        html += proximas.map(a => `
            <div class="appt-card appt-upcoming">
                ${avatarMedico(a, 48)}
                <div class="appt-info">
                    <div class="appt-medico">${a.nomeMedico || "—"}</div>
                    <div class="appt-data">${formatDate(a.dataHora)}</div>
                    <span class="appt-badge appt-${a.status.toLowerCase()}">${a.status}</span>
                </div>
                <button class="btn-cancelar" onclick="cancelarConsulta('${a.id}')">Cancelar</button>
            </div>`).join("");
    } else {
        html += `<div class="appt-empty-upcoming">
            <p>Nenhuma consulta agendada.</p>
            <a href="patient-marketplace.html" class="btn-primary" style="display:inline-block;margin-top:10px;text-decoration:none;padding:10px 20px">
                Agendar consulta
            </a>
        </div>`;
    }

    // ── Realizadas ────────────────────────────────────────────────────
    if (realizadas.length > 0) {
        html += `<p class="appt-section-label" style="margin-top:20px">Consultas realizadas</p>`;
        html += realizadas.map(a => `
            <div class="appt-card appt-past">
                ${avatarMedico(a, 36)}
                <div class="appt-info">
                    <div class="appt-medico" style="font-size:13px">${a.nomeMedico || "—"}</div>
                    <div class="appt-data" style="font-size:12px">${formatDate(a.dataHora)}</div>
                </div>
                <span class="appt-badge appt-realizado">Realizada</span>
            </div>`).join("");
    }

    // ── Canceladas (accordion) ────────────────────────────────────────
    if (canceladas.length > 0) {
        html += `
        <details class="appt-canceladas" style="margin-top:16px">
            <summary style="cursor:pointer;font-size:12px;color:var(--muted);font-weight:600;user-select:none;list-style:none;display:flex;align-items:center;gap:6px">
                <span>▶</span> ${canceladas.length} consulta${canceladas.length > 1 ? "s" : ""} cancelada${canceladas.length > 1 ? "s" : ""}
            </summary>
            <div style="margin-top:8px">
                ${canceladas.map(a => `
                <div class="appt-card appt-canceled">
                    ${avatarMedico(a, 32)}
                    <div class="appt-info">
                        <div class="appt-medico" style="font-size:12px;color:var(--muted)">${a.nomeMedico || "—"}</div>
                        <div class="appt-data" style="font-size:11px;color:var(--muted)">${formatDate(a.dataHora)}</div>
                    </div>
                </div>`).join("")}
            </div>
        </details>`;
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


function confirmarCancelamento(id, onConfirm) {
    const overlay = document.createElement("div");
    overlay.style.cssText = "position:fixed;inset:0;background:rgba(0,0,0,.45);z-index:3000;display:flex;align-items:center;justify-content:center";

    overlay.innerHTML = `
        <div style="background:#fff;border-radius:14px;padding:28px 24px;width:300px;text-align:center;box-shadow:0 8px 32px rgba(0,0,0,.25)">
            <p style="font-size:28px;margin-bottom:8px">⚠️</p>
            <p style="font-size:16px;font-weight:600;margin-bottom:20px">Cancelar esta consulta?</p>
            <div style="display:flex;gap:10px">
                <button id="_pp_nao" style="flex:1;padding:11px;border:1px solid #ddd;border-radius:8px;background:#f5f5f5;cursor:pointer;font-size:14px">Não</button>
                <button id="_pp_sim" style="flex:1;padding:11px;border:none;border-radius:8px;background:#c62828;color:#fff;cursor:pointer;font-size:14px;font-weight:600">Sim, cancelar</button>
            </div>
        </div>`;

    document.body.appendChild(overlay);
    overlay.querySelector("#_pp_nao").onclick = () => overlay.remove();
    overlay.querySelector("#_pp_sim").onclick = async () => {
        overlay.remove();
        await onConfirm(id);
    };
}

window.cancelarConsulta = function(id) {
    confirmarCancelamento(id, async (agId) => {
        const res = await apiFetch(`/appointments/${agId}`, { method: "DELETE" });
        if (res.ok) {
            showMsg("Consulta cancelada com sucesso.", "success");
            loadAppointments();
        } else {
            const err = await res.json().catch(() => ({}));
            showMsg(err.message || "Não foi possível cancelar a consulta.", "error");
        }
    });
};

const TOUR_PACIENTE = [
    {
        icon: "🎉",
        title: "Bem-vindo ao portal!",
        text:  "Este é o seu espaço pessoal de saúde. Aqui você gerencia todas as suas consultas médicas."
    },
    {
        icon: "🔍",
        title: "Agende uma consulta",
        target: "a[href='patient-marketplace.html']",
        text:  "Clique em \"Agendar nova consulta\" para encontrar médicos disponíveis, ver horários e fazer o agendamento online."
    },
    {
        icon: "📋",
        title: "Suas consultas",
        target: "#appointments-container",
        text:  "Aqui aparecem suas próximas consultas com foto do médico, data e horário. Consultas canceladas ficam escondidas para não poluir a tela."
    },
    {
        icon: "📒",
        title: "Histórico médico",
        target: "#prontuarios-container",
        text:  "Seus prontuários e histórico de atendimentos ficam registrados aqui para consulta futura."
    },
    {
        icon: "✅",
        title: "Tudo certo!",
        text:  "Agora você já sabe como funciona o portal. Explore e agende sua primeira consulta!"
    }
];

loadProfile();
loadAppointments();
loadProntuarios();

setTimeout(() => {
    const tour = new TourGuide("paciente-v1", TOUR_PACIENTE);
    tour.start();
}, 1000);
