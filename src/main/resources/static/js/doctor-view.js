const API   = "";
const token = localStorage.getItem("token");
const role  = localStorage.getItem("role");

if (!token || role !== "PACIENTE") { window.location.href = "patient-login.html"; }

const params  = new URLSearchParams(location.search);
const medicoId = params.get("id");
if (!medicoId) { window.location.href = "patient-marketplace.html"; }

async function apiFetch(path, opts = {}) {
    const res = await fetch(API + path, {
        ...opts,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + token, ...(opts.headers || {}) }
    });
    if (res.status === 401) { localStorage.clear(); window.location.href = "patient-login.html"; throw new Error(); }
    return res;
}

function currency(v) { return Number(v || 0).toLocaleString("pt-BR", { style: "currency", currency: "BRL" }); }
function showMsg(text, type) {
    const el = document.getElementById("book-message");
    el.textContent = text; el.className = "message " + type; el.classList.remove("hidden");
}

let selectedSlot = null;
let medicoNome   = "";

async function carregarMedico() {
    const res  = await apiFetch(`/patient/medicos/${medicoId}/detalhes`);
    if (!res.ok) { window.location.href = "patient-marketplace.html"; return; }
    const m = await res.json();

    medicoNome = m.nome || "";
    document.getElementById("page-title").textContent  = `Clínica — ${medicoNome}`;
    document.getElementById("dv-nome").textContent     = medicoNome;
    document.getElementById("dv-esp").textContent      = m.descricaoEspecialidade || m.especialidade || "";
    document.getElementById("dv-cidade").textContent   = m.cidade ? `📍 ${m.cidade}` : "";
    document.getElementById("dv-valor").textContent    = currency(m.valorConsulta);
    document.getElementById("dv-duracao").textContent  = `${m.duracaoConsultaMinutos || 60} minutos`;
    document.getElementById("dv-crm").textContent      = m.crm || "—";

    if (m.universidade) {
        const text = m.anoFormacao ? `${m.universidade} · ${m.anoFormacao}` : m.universidade;
        document.getElementById("dv-univ").textContent = text;
        document.getElementById("dv-stat-univ").style.display = "flex";
    }
    if (m.anosExperiencia != null) {
        document.getElementById("dv-exp").textContent = `${m.anosExperiencia} anos`;
        document.getElementById("dv-stat-exp").style.display = "flex";
    }
    if (m.descricao) {
        document.getElementById("dv-desc").textContent = m.descricao;
        document.getElementById("dv-card-desc").style.display = "block";
    }

    const avatar = document.getElementById("dv-avatar");
    if (m.fotoUrl) {
        avatar.innerHTML = `<img src="${m.fotoUrl}" alt="${medicoNome}">`;
    }

    const horariosEl = document.getElementById("dv-horarios");
    if (m.disponibilidade && m.disponibilidade.length > 0) {
        horariosEl.innerHTML = m.disponibilidade.map(d => `
            <div class="dv-horario-item">
                <span class="dia">${d.diaSemanaLabel}</span>
                <span>${d.horaInicio.slice(0,5)} – ${d.horaFim.slice(0,5)}</span>
            </div>`).join("");
    } else {
        horariosEl.innerHTML = "<p class=\"dv-empty\">Horários não informados.</p>";
    }

    const hoje = new Date().toISOString().split("T")[0];
    document.getElementById("book-date").min = hoje;
}

window.buscarSlots = async function() {
    const data = document.getElementById("book-date").value;
    if (!data) return;

    selectedSlot = null;
    document.getElementById("btn-agendar").disabled = true;
    const wrap  = document.getElementById("slots-wrap");
    const label = document.getElementById("slots-label");
    wrap.innerHTML = "<p class=\"dv-empty\">Buscando horários...</p>";

    const res   = await apiFetch(`/patient/medicos/${medicoId}/horarios?data=${data}`);
    const slots = await res.json();

    if (!Array.isArray(slots) || slots.length === 0) {
        label.style.display = "none";
        wrap.innerHTML = "<p class=\"dv-empty\">Sem horários disponíveis neste dia. Tente outra data.</p>";
        return;
    }

    label.style.display = "block";
    wrap.innerHTML = slots.map(s => {
        const hora = new Date(s).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
        return `<button class="slot-btn" onclick="selecionarSlot('${s}', this)">${hora}</button>`;
    }).join("");
};

window.selecionarSlot = function(slot, el) {
    document.querySelectorAll(".slot-btn").forEach(b => b.classList.remove("selected"));
    el.classList.add("selected");
    selectedSlot = slot;
    document.getElementById("btn-agendar").disabled = false;
};

window.abrirConfirmacao = function() {
    if (!selectedSlot) return;
    const dt = new Date(selectedSlot);
    const quando = dt.toLocaleDateString("pt-BR", { weekday: "long", day: "numeric", month: "long" })
                 + " às " + dt.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });

    const overlay = document.createElement("div");
    overlay.style.cssText = "position:fixed;inset:0;background:rgba(0,0,0,.45);z-index:3000;display:flex;align-items:center;justify-content:center";
    overlay.innerHTML = `
        <div style="background:#fff;border-radius:16px;padding:32px 28px;max-width:360px;width:90%;text-align:center">
            <p style="font-size:32px;margin-bottom:8px">🩺</p>
            <h3 style="font-size:18px;margin-bottom:6px">Confirmar agendamento?</h3>
            <p style="font-size:14px;color:#555;margin-bottom:4px"><strong>${medicoNome}</strong></p>
            <p style="font-size:14px;color:#555;margin-bottom:20px">${quando}</p>
            <p style="font-size:12px;color:#999;margin-bottom:20px">Cancelamento gratuito até 24h antes.</p>
            <div style="display:flex;gap:10px">
                <button id="_c_nao" style="flex:1;padding:12px;border:1px solid #ddd;border-radius:8px;background:#f5f5f5;cursor:pointer;font-size:14px">Cancelar</button>
                <button id="_c_sim" style="flex:1;padding:12px;background:#00897b;color:#fff;border:none;border-radius:8px;cursor:pointer;font-size:14px;font-weight:700">Confirmar</button>
            </div>
        </div>`;
    document.body.appendChild(overlay);

    overlay.querySelector("#_c_nao").onclick = () => overlay.remove();
    overlay.querySelector("#_c_sim").onclick = async () => {
        overlay.remove();
        await confirmarAgendamento();
    };
};

async function confirmarAgendamento() {
    const res  = await apiFetch("/patient/agendamentos", {
        method: "POST",
        body: JSON.stringify({ medicoId, dataHora: selectedSlot })
    });
    if (res.ok) {
        selectedSlot = null;
        document.getElementById("btn-agendar").disabled = true;
        document.querySelectorAll(".slot-btn").forEach(b => b.classList.remove("selected"));
        showMsg("✅ Consulta agendada com sucesso!", "success");
        setTimeout(() => document.getElementById("book-message").classList.add("hidden"), 5000);
        window.buscarSlots();
    } else {
        const err = await res.json().catch(() => ({}));
        showMsg(err.message || "Erro ao agendar.", "error");
    }
}

carregarMedico();
