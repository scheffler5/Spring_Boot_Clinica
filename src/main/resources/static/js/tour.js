/**
 * TourGuide — motor de tour passo a passo.
 *
 * Uso:
 *   const tour = new TourGuide('medico', steps);
 *   tour.start();
 *
 * Cada step: { icon, title, text, target?, action? }
 *   target  → seletor CSS do elemento a destacar
 *   action  → função chamada antes de mostrar o step (ex: ativar uma aba)
 */
class TourGuide {
    constructor(tourId, steps) {
        this.id      = tourId;
        this.steps   = steps;
        this.i       = 0;
        this._overlay = null;
        this._box     = null;
        this._hl      = null;
    }

    shouldRun() {
        return !localStorage.getItem(`tour_done_${this.id}`);
    }

    start() {
        if (!this.shouldRun()) return;
        this._build();
        this._show(0);
        window._activeTour = this;
    }

    _build() {
        this._overlay = document.createElement('div');
        this._overlay.className = 'tour-overlay';
        document.body.appendChild(this._overlay);

        this._box = document.createElement('div');
        this._box.className = 'tour-box';
        document.body.appendChild(this._box);
    }

    _show(index) {
        this.i = index;
        const step = this.steps[index];
        const total = this.steps.length;

        // Remove destaque anterior
        if (this._hl) { this._hl.classList.remove('tour-highlight'); this._hl = null; }

        // Aciona ação do step (ex: trocar aba)
        if (step.action) { step.action(); }

        // Destaca o elemento alvo
        if (step.target) {
            const el = document.querySelector(step.target);
            if (el) {
                this._hl = el;
                el.classList.add('tour-highlight');
                setTimeout(() => el.scrollIntoView({ behavior: 'smooth', block: 'center' }), 150);
            }
        }

        const isFirst = index === 0;
        const isLast  = index === total - 1;

        this._box.innerHTML = `
            <div class="tour-header">
                <span class="tour-step-count">Passo ${index + 1} de ${total}</span>
                <button class="tour-skip" onclick="window._activeTour.finish()">Pular tour</button>
            </div>
            <div class="tour-icon">${step.icon || '💡'}</div>
            <h3 class="tour-title">${step.title}</h3>
            <p class="tour-text">${step.text}</p>
            <div class="tour-dots">
                ${this.steps.map((_, j) =>
                    `<div class="tour-dot${j === index ? ' active' : ''}"></div>`
                ).join('')}
            </div>
            <div class="tour-actions">
                ${isFirst
                    ? '<span></span>'
                    : '<button class="tour-btn-prev" onclick="window._activeTour.prev()">← Anterior</button>'
                }
                ${isLast
                    ? '<button class="tour-btn-finish" onclick="window._activeTour.finish()">Concluir ✓</button>'
                    : '<button class="tour-btn-next" onclick="window._activeTour.next()">Próximo →</button>'
                }
            </div>`;
    }

    next()   { if (this.i < this.steps.length - 1) this._show(this.i + 1); }
    prev()   { if (this.i > 0)                     this._show(this.i - 1); }
    finish() {
        localStorage.setItem(`tour_done_${this.id}`, 'true');
        if (this._hl)      { this._hl.classList.remove('tour-highlight'); }
        if (this._overlay) { this._overlay.remove(); }
        if (this._box)     { this._box.remove(); }
        window._activeTour = null;
    }

    /** Permite reiniciar o tour (para debug / botão "Ver tour novamente") */
    static reset(tourId) {
        localStorage.removeItem(`tour_done_${tourId}`);
    }
}

window.TourGuide = TourGuide;
