const API   = "";
const token = localStorage.getItem("token");
const role  = localStorage.getItem("role");

if (!token) { window.location.href = role === "PACIENTE" ? "patient-login.html" : "index.html"; }

const meId = (() => {
    try { return JSON.parse(atob(token.split(".")[1])).id; } catch { return null; }
})();

async function apiFetch(path, opts = {}) {
    const res = await fetch(API + path, {
        ...opts,
        headers: { "Content-Type": "application/json", Authorization: "Bearer " + token, ...(opts.headers || {}) }
    });
    if (res.status === 401) { localStorage.clear(); window.location.href = "index.html"; throw new Error(); }
    return res;
}

function fmt(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    return d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}

let stompClient    = null;
let activeSub      = null;
let conversaAtiva  = null;
let pendingAnexo   = null; // { url, nome, tipo }

// ── Voltar ──────────────────────────────────────────────────────────
document.getElementById("btn-voltar").onclick = () =>
    window.location.href = role === "PACIENTE" ? "patient-dashboard.html" : "dashboard.html";

document.getElementById("btn-logout").onclick = () => {
    if (stompClient) stompClient.deactivate();
    localStorage.clear();
    window.location.href = role === "PACIENTE" ? "patient-login.html" : "index.html";
};

// ── Conectar WebSocket ───────────────────────────────────────────────
function conectarWS() {
    stompClient = new StompJs.Client({
        webSocketFactory: () => new SockJS(`${API}/ws`),
        connectHeaders: { Authorization: "Bearer " + token },
        reconnectDelay: 5000,
        onConnect: () => console.log("WS conectado"),
        onStompError: (e) => console.error("STOMP error", e)
    });
    stompClient.activate();
}

function inscreverConversa(conversaId) {
    if (activeSub) { activeSub.unsubscribe(); activeSub = null; }
    if (!stompClient?.connected) return;

    activeSub = stompClient.subscribe(`/topic/conversa/${conversaId}`, (msg) => {
        const m = JSON.parse(msg.body);
        appendMensagem(m, m.remetenteId === meId);
        scrollDown();
        atualizarPreview(conversaId, m);
    });
}

// ── Lista de conversas ───────────────────────────────────────────────
async function carregarConversas() {
    const res   = await apiFetch("/chat/conversas");
    const lista = await res.json();
    const el    = document.getElementById("chat-list");

    if (!Array.isArray(lista) || lista.length === 0) {
        el.innerHTML = '<div style="padding:20px;text-align:center;color:var(--muted);font-size:13px">Nenhuma conversa ainda.</div>';
        return;
    }

    el.innerHTML = lista.map(c => `
        <div class="chat-item" id="item-${c.id}" onclick="abrirConversa('${c.id}','${c.outroUsuarioNome}','${c.outroUsuarioFotoUrl||''}')">
            <div class="chat-item-avatar">
                ${c.outroUsuarioFotoUrl
                    ? `<img src="${c.outroUsuarioFotoUrl}">`
                    : '🩺'}
            </div>
            <div class="chat-item-info">
                <div class="chat-item-nome">${c.outroUsuarioNome}</div>
                <div class="chat-item-last">${c.ultimaMensagem || 'Sem mensagens ainda'}</div>
            </div>
            ${c.naoLidas > 0 ? `<div class="chat-badge">${c.naoLidas}</div>` : ''}
        </div>`).join("");

    // Abre conversa passada na URL
    const urlParams = new URLSearchParams(location.search);
    const idParam   = urlParams.get("id");
    const nomeParam = urlParams.get("nome");
    const fotoParam = urlParams.get("foto") || "";
    if (idParam) abrirConversa(idParam, nomeParam || "Conversa", fotoParam);
}

// ── Abrir conversa ───────────────────────────────────────────────────
async function abrirConversa(id, nome, foto) {
    conversaAtiva = id;

    document.querySelectorAll(".chat-item").forEach(el => el.classList.remove("active"));
    const item = document.getElementById(`item-${id}`);
    if (item) {
        item.classList.add("active");
        item.querySelector(".chat-badge")?.remove();
    }

    const main = document.getElementById("chat-main");
    const avatarHtml = foto
        ? `<img src="${foto}" style="width:38px;height:38px;border-radius:50%;object-fit:cover">`
        : `<div class="ch-avatar">🩺</div>`;

    pendingAnexo = null;
    main.innerHTML = `
        <div class="chat-main-header">
            ${avatarHtml}
            <div class="chat-main-nome">${nome}</div>
        </div>
        <div class="chat-messages" id="msgs"></div>
        <div id="anexo-preview" style="display:none;padding:8px 20px;background:#f0f4f8;border-top:1px solid var(--border);display:none;align-items:center;gap:10px">
            <span id="anexo-info" style="flex:1;font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis"></span>
            <button onclick="removerAnexo()" style="background:none;border:none;color:#e53935;cursor:pointer;font-size:18px;padding:0">✕</button>
        </div>
        <div class="chat-input-row">
            <button class="chat-attach-btn" title="Anexar arquivo" onclick="document.getElementById('chat-file-input').click()">📎</button>
            <input type="file" id="chat-file-input" style="display:none" accept="image/*,.pdf,.doc,.docx" onchange="selecionarAnexo(this)">
            <textarea class="chat-input" id="chat-input" placeholder="Digite uma mensagem..." rows="1"
                onkeydown="if(event.key==='Enter'&&!event.shiftKey){event.preventDefault();enviar()}"
                oninput="this.style.height='auto';this.style.height=Math.min(this.scrollHeight,100)+'px'"></textarea>
            <button class="chat-send" id="btn-send" onclick="enviar()">➤</button>
        </div>`;

    const res  = await apiFetch(`/chat/conversas/${id}/mensagens`);
    const msgs = await res.json();
    msgs.forEach(m => appendMensagem(m, m.remetenteId === meId));
    scrollDown();

    apiFetch(`/chat/conversas/${id}/lidas`, { method: "PATCH" });

    inscreverConversa(id);
    document.getElementById("chat-input").focus();
}

// ── Anexos ────────────────────────────────────────────────────────────
async function selecionarAnexo(input) {
    const file = input.files[0];
    if (!file) return;
    const preview = document.getElementById("anexo-preview");
    const info    = document.getElementById("anexo-info");
    if (preview) { preview.style.display = "flex"; }
    if (info)    { info.textContent = "⏳ Enviando " + file.name + "..."; }

    const form = new FormData();
    form.append("arquivo", file);
    try {
        const res = await fetch(`${API}/chat/upload`, {
            method: "POST",
            headers: { Authorization: "Bearer " + token },
            body: form
        });
        if (!res.ok) { if (info) info.textContent = "❌ Erro no upload"; return; }
        const data = await res.json();
        pendingAnexo = data;
        const icon = data.tipo === "imagem" ? "🖼" : "📄";
        if (info) info.textContent = icon + " " + data.nome;
    } catch {
        if (info) info.textContent = "❌ Erro no upload";
        pendingAnexo = null;
    }
    input.value = "";
}

function removerAnexo() {
    pendingAnexo = null;
    const preview = document.getElementById("anexo-preview");
    if (preview) preview.style.display = "none";
}

// ── Enviar mensagem ──────────────────────────────────────────────────
function enviar() {
    if (!conversaAtiva || !stompClient?.connected) return;
    const input = document.getElementById("chat-input");
    const texto = input.value.trim() || "";
    if (!texto && !pendingAnexo) return;

    const payload = { texto };
    if (pendingAnexo) {
        payload.imagemUrl = pendingAnexo.url;
        payload.nomeAnexo = pendingAnexo.nome;
    }

    stompClient.publish({
        destination: `/app/chat/${conversaAtiva}`,
        body: JSON.stringify(payload)
    });

    input.value = "";
    input.style.height = "auto";
    removerAnexo();
}

// ── Renderizar mensagem ──────────────────────────────────────────────
function appendMensagem(m, euSou) {
    const container = document.getElementById("msgs");
    if (!container) return;

    let conteudo = "";
    if (m.imagemUrl) {
        const nome = m.nomeAnexo || "arquivo";
        const ext  = nome.split(".").pop().toLowerCase();
        const isImg = ["jpg","jpeg","png","webp","gif"].includes(ext);
        if (isImg) {
            conteudo += `<img src="${m.imagemUrl}" style="max-width:100%;max-height:240px;border-radius:10px;display:block;margin-bottom:4px;cursor:pointer" onclick="window.open('${m.imagemUrl}','_blank')">`;
        } else {
            conteudo += `<a href="${m.imagemUrl}" target="_blank" style="display:flex;align-items:center;gap:8px;color:inherit;text-decoration:none;padding:6px;background:rgba(255,255,255,.15);border-radius:8px;margin-bottom:4px"><span style="font-size:24px">📄</span><span style="font-size:13px;text-decoration:underline">${escapeHtml(nome)}</span></a>`;
        }
    }
    if (m.texto) conteudo += escapeHtml(m.texto);

    const div = document.createElement("div");
    div.innerHTML = `
        ${!euSou ? `<div class="msg-nome-remetente">${m.remetenteNome}</div>` : ""}
        <div class="msg-bubble ${euSou ? 'eu' : 'outro'}">
            ${conteudo}
            <span class="msg-hora">${fmt(m.timestamp)}</span>
        </div>`;
    container.appendChild(div);
}

function scrollDown() {
    const el = document.getElementById("msgs");
    if (el) el.scrollTop = el.scrollHeight;
}

function atualizarPreview(id, m) {
    const item = document.getElementById(`item-${id}`);
    if (item) {
        const last = item.querySelector(".chat-item-last");
        if (!last) return;
        if (m.imagemUrl) {
            const nome = m.nomeAnexo || "arquivo";
            const ext  = nome.split(".").pop().toLowerCase();
            last.textContent = ["jpg","jpeg","png","webp","gif"].includes(ext) ? "🖼 " + nome : "📄 " + nome;
        } else {
            const texto = m.texto || "";
            last.textContent = texto.length > 40 ? texto.slice(0, 40) + "..." : texto;
        }
    }
}

function escapeHtml(t) {
    return t.replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;")
            .replace(/\n/g,"<br>");
}

// ── Carregar nome do usuário ─────────────────────────────────────────
apiFetch("/medico/perfil").then(r => r.json()).then(d => {
    if (d.nome) document.getElementById("topbar-nome").textContent = d.nome;
}).catch(() => {
    apiFetch("/patient/me").then(r => r.json()).then(d => {
        if (d.nome) document.getElementById("topbar-nome").textContent = d.nome;
    }).catch(() => {});
});

// ── Init ─────────────────────────────────────────────────────────────
conectarWS();
carregarConversas();
