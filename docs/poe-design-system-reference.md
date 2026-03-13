# Poe.com Design System — Referência Completa para Frontend Lume

> Reconstituído via análise de CSS variables, tokens e componentes reais do poe.com.
> Prefixo do design system: `--pdl-*` (Poe Design Language)

---

## 1. Paleta de Cores — Tokens Semânticos

### Light Mode (default)

| Token | Valor | Uso |
|-------|-------|-----|
| `--pdl-bg-base` | `#FFFFFF` | Background principal |
| `--pdl-bg-faint` | `#F7F7F7` | Background sutil (sidebar, header) |
| `--pdl-bg-subtle` | `#F1F2F2` | Background de cards, hover |
| `--pdl-fg-base` | `#0D0D0D` | Texto principal |
| `--pdl-fg-subtle` | `#505157` | Texto secundário |
| `--pdl-fg-muted` | `#6E6E72` | Labels, placeholders, meta text |
| `--pdl-border-base` | `#E4E7E7` | Bordas padrão |
| `--pdl-border-strong` | `#CFD3D3` | Bordas de botões outline |

### Dark Mode

| Token | Valor | Uso |
|-------|-------|-----|
| `--pdl-bg-base` | `#101012` | Background principal |
| `--pdl-bg-faint` | `#1B1B1F` | Background sutil (sidebar, header) |
| `--pdl-bg-subtle` | `#24292E` | Background de cards, code blocks |
| `--pdl-fg-base` | `#FCFCFC` | Texto principal |
| `--pdl-fg-subtle` | `#E1E4E8` | Texto secundário |
| `--pdl-fg-muted` | `#8B8B8F` | Labels, placeholders |
| `--pdl-border-base` | `#2E2E32` | Bordas padrão |
| `--pdl-border-strong` | `#3E3E42` | Bordas de botões outline |

### Escala Neutral (neutral-1 a neutral-12)

| Token | Light | Dark |
|-------|-------|------|
| `--neutral-1` | `#FFFFFF` | `#101012` |
| `--neutral-2` | `#F7F7F7` | `#1B1B1F` |
| `--neutral-3` | `#F1F2F2` | `#24292E` |
| `--neutral-4` | `#E4E7E7` | `#2E2E32` |
| `--neutral-5` | `#D5D8D8` | `#3E3E42` |
| `--neutral-6` | `#CFD3D3` | `#505054` |
| `--neutral-7` | `#A8ACAC` | `#636367` |
| `--neutral-8` | `#8B8F8F` | `#7A7A7E` |
| `--neutral-9` | `#6E6E72` | `#8B8B8F` |
| `--neutral-10` | `#505157` | `#A0A0A4` |
| `--neutral-11` | `#333338` | `#C8C8CC` |
| `--neutral-12` | `#0D0D0D` | `#FCFCFC` |

### Cores de Accent — Violet (primária)

| Token | Valor |
|-------|-------|
| `--violet-1` | `#FDFCFE` |
| `--violet-2` | `#FBF8FF` |
| `--violet-3` | `#F4EEFF` |
| `--violet-4` | `#E9DEFF` |
| `--violet-5` | `#DCCCFF` |
| `--violet-6` | `#C9B0FF` |
| `--violet-7` | `#AE8EFC` |
| `--violet-8` | `#8E6CEF` |
| `--violet-9` | `#5D5CDE` | Accent principal |
| `--violet-10` | `#5352C7` | Hover accent |
| `--violet-11` | `#3B3ABE` | Accent escuro |
| `--violet-12` | `#1F1F4E` | Accent muito escuro |

### Cores de Status

| Propósito | Token | Light | Dark |
|-----------|-------|-------|------|
| Sucesso | `--teal-9` | `#1CDDAE` | `#1CDDAE` |
| Erro | `--ruby-9` | `#E54666` | `#E54666` |
| Warning | `--yellow-9` | `#FFB224` | `#FFB224` |
| Info | `--blue-9` | `#3E63DD` | `#3E63DD` |

---

## 2. Tipografia

### Font Families

```css
/* Body text — system font stack */
--pdl-font-families-body-text-base-normal:
  -apple-system, system-ui, BlinkMacSystemFont,
  "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;

/* Headings — mesma stack com peso diferente */
--pdl-font-families-heading-base-bold:
  -apple-system, system-ui, BlinkMacSystemFont,
  "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;

/* Meta/secondary text */
--pdl-font-families-meta-text:
  -apple-system, system-ui, BlinkMacSystemFont,
  "Segoe UI", Roboto, sans-serif;

/* Code / monospace */
--pdl-font-families-code:
  SFMono-Regular, Consolas, "Liberation Mono",
  Menlo, monaco, monospace;
```

### Escala Tipográfica

| Elemento | Tamanho | Peso | Line-height | Letter-spacing |
|----------|---------|------|-------------|----------------|
| Display / H1 | 1.5rem (24px) | 700 (bold) | 1.2 | -0.5px |
| H2 | 1.25rem (20px) | 700 | 1.25 | -0.3px |
| H3 | 1.125rem (18px) | 600 (semibold) | 1.3 | normal |
| Body | 1rem (16px) | 400 (regular) | 1.5 | normal |
| Body small | 0.875rem (14px) | 400 | 1.4 | normal |
| Caption / Meta | 0.75rem (12px) | 500 (medium) | 1.35 | 0.02em |
| Button text | 0.875rem (14px) | 600 | 1.0 | normal |

---

## 3. Spacing System

Base unit: **8px** (0.5rem)

| Token | Valor |
|-------|-------|
| `--pdl-spacing-0` | 0rem |
| `--pdl-spacing-xxx-sm` | 0.125rem (2px) |
| `--pdl-spacing-xx-sm` | 0.25rem (4px) |
| `--pdl-spacing-x-sm` | 0.5rem (8px) |
| `--pdl-spacing-sm` | 0.75rem (12px) |
| `--pdl-spacing-md` | 1rem (16px) |
| `--pdl-spacing-lg` | 1.25rem (20px) |
| `--pdl-spacing-x-lg` | 1.5rem (24px) |
| `--pdl-spacing-xx-lg` | 2rem (32px) |
| `--pdl-spacing-xxx-lg` | 2.5rem (40px) |

---

## 4. Border Radius

| Token | Valor | Uso |
|-------|-------|-----|
| `--pdl-sizing-radius-sm` | 0.25rem (4px) | Badges, tags |
| `--pdl-sizing-radius-md` | 0.5rem (8px) | Inputs, cards padrão |
| `--pdl-sizing-radius-lg` | 0.75rem (12px) | Message bubbles, cards maiores |
| `--pdl-sizing-radius-xl` | 1rem (16px) | Modais |
| `--pdl-sizing-radius-pill` | 9999px | Botões, chips, input de chat |
| Avatar circle | `50%` | Avatares de bots |
| Avatar rounded | `30%` | Avatares alternativos |

---

## 5. Shadows

| Token | Valor | Uso |
|-------|-------|-----|
| `--pdl-box-shadow-base` | `0 0 6px 1px rgba(0,0,0,0.1)` | Sombra padrão |
| `--pdl-box-shadow-hover` | `0 4px 12px rgba(0,0,0,0.08)` | Hover de cards |
| `--pdl-box-shadow-elevated` | `0 8px 24px rgba(0,0,0,0.12)` | Dropdowns, modais |
| Focus ring inner | `0 0 0 1px var(--violet-9)` | Focus state (inner) |
| Focus ring outer | `0 0 0 2px var(--violet-5)` | Focus state (outer) |

---

## 6. Z-Index System

| Token | Valor | Uso |
|-------|-------|-----|
| `--z-index-base` | 0 | Conteúdo normal |
| `--z-index-elevated` | 1 | Elementos elevados |
| `--z-index-floating` | 2 | Tooltips, popovers |
| `--z-index-overlay` | 3 | Overlays |
| `--z-index-sidebar` | 4 | Sidebar (mobile) |
| `--z-index-modal` | 5 | Modais |
| `--z-index-toast` | 51 | Toasts / notificações |

---

## 7. Layout Principal

### Estrutura

```
┌─────────────────────────────────────────────────────────┐
│           Header Bar (h: 3.125rem / 50px)               │
│  [☰ Toggle]  [     Search / Model Select     ]  [User] │
├──────────┬──────────────────────────────────────────────┤
│          │                                               │
│ Sidebar  │              Main Content                     │
│          │              (flex-1)                          │
│ expanded:│                                               │
│ 18.75rem │  ┌──────────────────────────────────┐        │
│ (300px)  │  │        max-width: 48rem          │        │
│          │  │        (768px) centered           │        │
│ compact: │  │                                   │        │
│ 4.25rem  │  │        Chat / Explore / etc       │        │
│ (68px)   │  │                                   │        │
│          │  └──────────────────────────────────┘        │
└──────────┴──────────────────────────────────────────────┘
```

### Header

```css
.Header {
  height: var(--header-height); /* 3.125rem = 50px */
  background: var(--pdl-bg-faint);
  border-bottom: 1px solid var(--pdl-border-base);
  display: grid;
  grid-template-columns: 1fr 2fr 1fr; /* left / center / right */
  align-items: center;
  padding: 0 var(--pdl-spacing-md);
}
```

### Sidebar

```css
.Sidebar {
  width: var(--default-sidebar-width); /* 18.75rem = 300px */
  background: var(--pdl-bg-faint);
  border-right: 1px solid var(--pdl-border-base);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  transition: width 0.3s ease-in-out;
}

.Sidebar--compact {
  width: var(--compact-sidebar-width); /* 4.25rem = 68px */
}
```

### Sidebar Items

```css
.SidebarItem {
  display: flex;
  align-items: center;
  gap: var(--pdl-spacing-sm); /* 12px */
  padding: var(--pdl-spacing-x-sm) var(--pdl-spacing-md); /* 8px 16px */
  border-radius: var(--pdl-sizing-radius-md); /* 8px */
  color: var(--pdl-fg-base);
  cursor: pointer;
  transition: background 0.15s ease;
}

.SidebarItem:hover {
  background-image: var(--button-hover); /* semi-transparent overlay */
}

.SidebarItem--active {
  background: var(--violet-3); /* #F4EEFF light / equivalente dark */
  color: var(--violet-11); /* #3B3ABE */
}
```

### Itens do Sidebar (estrutura)

```
┌──────────────────────┐
│  🏠 Home             │  ← Ícone outlined 20px + label
│  🔍 Explore          │
│  ➕ Create Bot       │
│  ─────────────────── │  ← Divider
│  📜 Your Bots        │  ← Seção colapsável
│    Bot 1             │
│    Bot 2             │
│  ─────────────────── │
│  💬 Recent Chats     │  ← Seção colapsável
│    Chat com GPT-5    │
│    Chat com Claude   │
│  ─────────────────── │
│  👤 Profile          │  ← Bottom section
│  ⚙️ Settings         │
└──────────────────────┘
```

### Breakpoints

| Nome | Valor | Comportamento |
|------|-------|---------------|
| Mobile | `max-width: 999px` | Sidebar overlay, padding reduzido |
| Tablet | `1000px - 1200px` | Sidebar compact (68px) |
| Desktop | `> 1200px` | Sidebar expandida (300px) |

---

## 8. Componentes

### Botões

```css
/* Variantes */
.button_primary {
  background: var(--violet-9); /* #5D5CDE */
  color: #FFFFFF;
  border: none;
  border-radius: var(--pdl-sizing-radius-pill); /* 9999px */
  font-weight: 600;
  font-size: 0.875rem;
  height: 2rem; /* 32px */
  padding: 0 var(--pdl-spacing-md); /* 0 16px */
  gap: var(--pdl-spacing-xx-sm); /* 4px */
  cursor: pointer;
  transition: background 0.15s ease;
}
.button_primary:hover {
  background: var(--violet-10); /* #5352C7 */
}

.button_secondary {
  background: var(--pdl-bg-subtle);
  color: var(--pdl-fg-base);
  border: none;
  border-radius: var(--pdl-sizing-radius-pill);
}

.button_outline {
  background: transparent;
  color: var(--pdl-fg-base);
  border: 1px solid var(--pdl-border-strong); /* #CFD3D3 */
  border-radius: var(--pdl-sizing-radius-pill);
}

.button_ghost {
  background: transparent;
  color: var(--pdl-fg-subtle);
  border: none;
  border-radius: var(--pdl-sizing-radius-pill);
}

/* Tamanhos */
.button--x-sm { height: 1.5rem; font-size: 0.75rem; padding: 0 0.5rem; }
.button--sm   { height: 2rem;   font-size: 0.875rem; padding: 0 0.75rem; }
.button--md   { height: 2.5rem; font-size: 0.875rem; padding: 0 1rem; }
```

### Input de Chat (Composer)

```css
.ChatComposer {
  position: sticky;
  bottom: 0;
  background: var(--pdl-bg-base);
  padding: var(--pdl-spacing-sm) var(--pdl-spacing-md);
  border-top: 1px solid var(--pdl-border-base);
}

.ChatComposer__textarea {
  width: 100%;
  min-height: var(--pdl-sizing-input-height-xx-lg); /* 4.5rem = 72px */
  max-height: 12rem;
  border: 1px solid var(--pdl-border-base);
  border-radius: var(--pdl-sizing-radius-lg); /* 12px */
  padding: var(--pdl-spacing-sm) var(--pdl-spacing-md);
  font-family: var(--pdl-font-families-body-text-base-normal);
  font-size: 1rem;
  resize: none;
  background: var(--pdl-bg-base);
  color: var(--pdl-fg-base);
  transition: border-color 0.15s ease;
}

.ChatComposer__textarea:focus {
  border-color: var(--violet-9);
  outline: none;
  box-shadow: 0 0 0 2px var(--violet-5);
}

.ChatComposer__send {
  position: absolute;
  right: 1.5rem;
  bottom: 1.5rem;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background: var(--violet-9);
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
}
```

### Message Bubbles

```css
.MessageBubble--prompt {
  background: var(--pdl-comp-message-bubble-theme-prompt-bg); /* violet accent sutil */
  color: var(--pdl-fg-base);
  border-radius: var(--pdl-sizing-radius-lg) var(--pdl-sizing-radius-lg) var(--pdl-sizing-radius-sm) var(--pdl-sizing-radius-lg);
  padding: var(--pdl-spacing-x-sm) var(--pdl-spacing-md); /* 8px 16px */
  max-width: 80%;
  align-self: flex-end;
}

.MessageBubble--response {
  background: var(--pdl-comp-message-bubble-theme-response-bg); /* faint bg */
  color: var(--pdl-fg-base);
  border-radius: var(--pdl-sizing-radius-lg) var(--pdl-sizing-radius-lg) var(--pdl-sizing-radius-lg) var(--pdl-sizing-radius-sm);
  padding: var(--pdl-spacing-x-sm) var(--pdl-spacing-md);
  max-width: 80%;
  align-self: flex-start;
}
```

### Message Actions (Like/Dislike/Copy)

```css
.MessageActions {
  display: flex;
  gap: var(--pdl-spacing-xx-sm); /* 4px */
  margin-top: var(--pdl-spacing-xx-sm);
  opacity: 0;
  transition: opacity 0.15s ease;
}

.MessageBubble:hover .MessageActions {
  opacity: 1;
}

.MessageAction__button {
  width: 1.75rem;
  height: 1.75rem;
  border-radius: var(--pdl-sizing-radius-md);
  background: transparent;
  color: var(--pdl-fg-muted);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.MessageAction__button:hover {
  background: var(--pdl-bg-subtle);
  color: var(--pdl-fg-base);
}
```

### Code Blocks

```css
.CodeBlock {
  background: #181818;
  border-radius: var(--pdl-sizing-radius-md); /* 8px */
  overflow: hidden;
  margin: var(--pdl-spacing-x-sm) 0;
}

.CodeBlock__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--pdl-spacing-xx-sm) var(--pdl-spacing-sm);
  background: #242424;
  color: #A0A0A4;
  font-size: 0.75rem;
  font-family: var(--pdl-font-families-code);
}

.CodeBlock__content {
  padding: var(--pdl-spacing-sm) var(--pdl-spacing-md);
  font-family: var(--pdl-font-families-code);
  font-size: 0.875rem;
  line-height: 1.5;
  color: #E1E4E8;
  overflow-x: auto;
  tab-index: 0; /* acessibilidade */
}
```

### Bot Cards (Explore Page)

```css
.BotCard {
  display: flex;
  align-items: flex-start;
  gap: var(--pdl-spacing-sm); /* 12px */
  padding: var(--pdl-spacing-x-sm) var(--pdl-spacing-md); /* 8px 16px */
  border-radius: var(--pdl-sizing-radius-md); /* 8px */
  cursor: pointer;
  transition: background 0.15s ease;
}

.BotCard:hover {
  background-image: var(--button-hover); /* gradient overlay */
}

.BotCard__avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  flex-shrink: 0;
}

.BotCard__name {
  font-weight: 600;
  font-size: 1rem;
  color: var(--pdl-fg-base);
}

.BotCard__description {
  font-size: 0.875rem;
  color: var(--pdl-fg-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
```

### Category/Filter Chips

```css
.FilterChip {
  display: inline-flex;
  align-items: center;
  height: 2rem; /* 32px */
  padding: 0 var(--pdl-spacing-sm); /* 0 12px */
  border-radius: var(--pdl-sizing-radius-pill); /* 9999px */
  font-size: 0.875rem;
  font-weight: 600;
  background: var(--pdl-bg-subtle);
  color: var(--pdl-fg-base);
  cursor: pointer;
  transition: all 0.15s ease;
}

.FilterChip--active {
  background: var(--pdl-fg-base);  /* inversão */
  color: var(--pdl-bg-base);
}
```

### Badges

```css
.Badge {
  display: inline-flex;
  padding: 0.125rem 0.375rem; /* 2px 6px */
  border-radius: var(--pdl-sizing-radius-sm); /* 4px */
  background: var(--neutral-3);
  color: var(--pdl-fg-subtle);
  font-size: 0.625rem; /* 10px */
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.Badge--official {
  background: var(--violet-3);
  color: var(--violet-11);
}

.Badge--new {
  background: var(--teal-3);
  color: var(--teal-11);
}
```

### Avatar

```css
.Avatar {
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.Avatar--xx-sm { width: 1.25rem; height: 1.25rem; }  /* 20px */
.Avatar--x-sm  { width: 1.5rem;  height: 1.5rem; }   /* 24px */
.Avatar--sm    { width: 2rem;    height: 2rem; }       /* 32px */
.Avatar--md    { width: 2.5rem;  height: 2.5rem; }     /* 40px */
.Avatar--lg    { width: 3rem;    height: 3rem; }        /* 48px */
.Avatar--x-lg  { width: 4rem;    height: 4rem; }        /* 64px */

.Avatar--rounded { border-radius: 30%; }
.Avatar--bordered { box-shadow: inset 0 0 0 1px rgba(0,0,0,0.08); }
```

### Loading Spinner

```css
.LoadingSpinner {
  border: 2px solid var(--pdl-border-base);
  border-top-color: var(--violet-9);
  border-radius: 50%;
  animation: spin var(--pdl-comp-loading-spinner-speed, 1s) linear infinite;
}

.LoadingSpinner--sm { width: 1rem; height: 1rem; }
.LoadingSpinner--md { width: 1.5rem; height: 1.5rem; }
.LoadingSpinner--lg { width: 2.5rem; height: 2.5rem; }

@keyframes spin {
  to { transform: rotate(360deg); }
}
```

### Typing Indicator (3 dots)

```css
.TypingIndicator {
  display: flex;
  gap: 4px;
  padding: 8px 12px;
}

.TypingIndicator__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--pdl-fg-muted);
  animation: typingPulse 1.4s infinite ease-in-out;
}

.TypingIndicator__dot:nth-child(2) { animation-delay: 0.2s; }
.TypingIndicator__dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes typingPulse {
  0%, 60%, 100% { transform: scale(0.6); opacity: 0.4; }
  30% { transform: scale(1); opacity: 1; }
}
```

---

## 9. Animações & Transições

| Efeito | CSS |
|--------|-----|
| Hover geral | `transition: all 0.15s ease` |
| Background hover | `transition: background 0.15s ease` |
| Opacity reveal | `transition: opacity 0.15s ease` |
| Sidebar collapse | `transition: width 0.3s ease-in-out` |
| Message fade-in | `animation: fadeInUp 0.2s ease-out` |
| Spinner | `animation: spin 1s linear infinite` |
| Typing dots | `animation: typingPulse 1.4s ease-in-out infinite` |

```css
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
```

---

## 10. Iconografia

- Estilo: **Outlined** (similar a Lucide/Feather icons)
- Stroke width: 1.5px
- Tamanhos:
  - 16px — ícones inline, metadata
  - 20px — ícones de sidebar, ações
  - 24px — ícones de heading, navegação
- Cor: herda `currentColor`

---

## 11. Tema System

```css
/* Implementação via media query + localStorage */
@media (prefers-color-scheme: dark) {
  :root { /* dark tokens */ }
}

/* Override manual via atributo ou classe */
[data-theme="light"] { /* light tokens */ }
[data-theme="dark"]  { /* dark tokens */ }
```

- localStorage key: `"theme"`
- Valores: `"light"` | `"dark"` | `"system"` (default)

---

## 12. Responsividade

| Breakpoint | Comportamento |
|------------|---------------|
| `< 640px` | Mobile: sidebar hidden, padding 8px-12px, single column |
| `640px - 999px` | Tablet: sidebar overlay on demand |
| `1000px - 1200px` | Desktop compact: sidebar 68px (ícones only) |
| `> 1200px` | Desktop full: sidebar 300px expandida |

---

## 13. Bot Info Page (Detalhe)

```
┌─────────────────────────────────────┐
│  [← Back]            [Share] [···]  │
├─────────────────────────────────────┤
│                                     │
│    [Avatar 80px]                    │
│    Bot Name          [OFFICIAL]     │
│    @creator_handle                  │
│    42.3K followers                  │
│                                     │
│    [Follow]  [Share]                │
│                                     │
│    Description text expandable...   │
│    ──────────────────────────       │
│                                     │
│    [Start Chat]  ← primary button   │
│                                     │
└─────────────────────────────────────┘
```

---

## 14. Pricing Page (Referência)

- Free tier vs Subscription ($19.99/mo)
- Comparison table com check marks
- Purple accent para plano recomendado
- Cards com `border-radius: 12px`
- Plano destaque: `border: 2px solid var(--violet-9)`
- Badge "POPULAR" em `var(--violet-3)` + `var(--violet-11)`

---

## 15. Changelog de Design Relevante (2025)

| Data | Mudança |
|------|---------|
| 2025-03-01 | Default CSS trocado de Flowbite para vanilla Tailwind |
| 2025-03-04 | Canvas view default em mobile (não chat) |
| 2025-03-11 | Streaming responses exibem imediatamente |
| 2025-03-13 | Otimizações de rendering para conversas longas |
| 2025-05-30 | App Creator gera apps com UI melhorada |
| 2025-11-11 | Design, styling e layout aprimorados nos Apps |
