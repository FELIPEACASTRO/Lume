"use client";

import { FormEvent } from "react";
import { Panel } from "@/components/ui/Panel";

type Props = {
  appearance: string;
  languageCode: string;
  emailUpdates: boolean;
  productUpdates: boolean;
  isSaving: boolean;
  feedback: string | null;
  onAppearanceChange: (value: string) => void;
  onLanguageCodeChange: (value: string) => void;
  onEmailUpdatesChange: (value: boolean) => void;
  onProductUpdatesChange: (value: boolean) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

export function SettingsPreferencesPanel({
  appearance,
  languageCode,
  emailUpdates,
  productUpdates,
  isSaving,
  feedback,
  onAppearanceChange,
  onLanguageCodeChange,
  onEmailUpdatesChange,
  onProductUpdatesChange,
  onSubmit
}: Props) {
  return (
    <Panel title="Preferencias" subtitle="Ajustes pessoais persistidos no backend.">
      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          Aparencia
          <select value={appearance} onChange={(event) => onAppearanceChange(event.target.value)}>
            <option value="light">Claro</option>
            <option value="dark">Escuro</option>
          </select>
        </label>
        <label>
          Idioma
          <input value={languageCode} onChange={(event) => onLanguageCodeChange(event.target.value)} />
        </label>
        <label>
          <input
            type="checkbox"
            checked={emailUpdates}
            onChange={(event) => onEmailUpdatesChange(event.target.checked)}
          />
          Receber atualizacoes por e-mail
        </label>
        <label>
          <input
            type="checkbox"
            checked={productUpdates}
            onChange={(event) => onProductUpdatesChange(event.target.checked)}
          />
          Receber avisos de produto
        </label>
        <div className="field-inline" style={{ gridColumn: "1 / -1" }}>
          <button className="button-primary" type="submit" disabled={isSaving}>
            {isSaving ? "Salvando..." : "Salvar preferencias"}
          </button>
        </div>
      </form>
      {feedback ? <p className="message-info">{feedback}</p> : null}
    </Panel>
  );
}
