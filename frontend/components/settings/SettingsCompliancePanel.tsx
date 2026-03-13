"use client";

import { FormEvent } from "react";
import { Panel } from "@/components/ui/Panel";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { SettingsComplianceSummary, UiOptionItem } from "@/lib/api/types";

type Props = {
  compliance: SettingsComplianceSummary;
  retentionPolicyStatus: string;
  retentionDays: string;
  accessReviewStatus: string;
  accessReviewFrequencyDays: string;
  consentTrackingEnabled: boolean;
  termsVersion: string;
  retentionStatusOptions: UiOptionItem[];
  accessReviewStatusOptions: UiOptionItem[];
  isSaving: boolean;
  feedback: string | null;
  onRetentionPolicyStatusChange: (value: string) => void;
  onRetentionDaysChange: (value: string) => void;
  onAccessReviewStatusChange: (value: string) => void;
  onAccessReviewFrequencyDaysChange: (value: string) => void;
  onConsentTrackingEnabledChange: (value: boolean) => void;
  onTermsVersionChange: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

export function SettingsCompliancePanel({
  compliance,
  retentionPolicyStatus,
  retentionDays,
  accessReviewStatus,
  accessReviewFrequencyDays,
  consentTrackingEnabled,
  termsVersion,
  retentionStatusOptions,
  accessReviewStatusOptions,
  isSaving,
  feedback,
  onRetentionPolicyStatusChange,
  onRetentionDaysChange,
  onAccessReviewStatusChange,
  onAccessReviewFrequencyDaysChange,
  onConsentTrackingEnabledChange,
  onTermsVersionChange,
  onSubmit
}: Props) {
  return (
    <Panel title="Compliance e controles" subtitle="Flags reais, politicas do workspace e rastreio operacional.">
      <div className="item-list">
        <div className="item-row">
          <strong>Webhook e trilha</strong>
          <div className="field-inline" style={{ marginTop: "0.45rem", flexWrap: "wrap" }}>
            <StatusBadge
              label={compliance.billingWebhookSecretConfigured ? "Webhook assinado" : "Webhook sem segredo"}
              variant={compliance.billingWebhookSecretConfigured ? "active" : "attention"}
            />
            <StatusBadge
              label={compliance.auditTrailEnabled ? "Audit trail ativo" : "Audit trail indisponivel"}
              variant={compliance.auditTrailEnabled ? "active" : "unavailable"}
            />
          </div>
        </div>
        <div className="item-row">
          <strong>Restricoes sensiveis</strong>
          <div className="field-inline" style={{ marginTop: "0.45rem", flexWrap: "wrap" }}>
            <StatusBadge
              label={compliance.darkWebMonitoringEnabled ? "Dark web habilitado" : "Dark web desligado"}
              variant={compliance.darkWebMonitoringEnabled ? "attention" : "restricted"}
            />
            <StatusBadge
              label={compliance.threatIntelRestrictedToAdmins ? "Threat-intel admin-only" : "Threat-intel exposto"}
              variant={compliance.threatIntelRestrictedToAdmins ? "active" : "unavailable"}
            />
            <StatusBadge
              label={compliance.byokValidationEnabled ? "BYOK validavel" : "BYOK indisponivel"}
              variant={compliance.byokValidationEnabled ? "active" : "unavailable"}
            />
            <StatusBadge
              label={compliance.consentTrackingEnabled ? "Consentimento rastreado" : "Consentimento sem rastreio"}
              variant={compliance.consentTrackingEnabled ? "active" : "attention"}
            />
          </div>
        </div>
      </div>
      <form className="form-grid" onSubmit={onSubmit}>
        <label>
          Politica de retencao
          <select value={retentionPolicyStatus} onChange={(event) => onRetentionPolicyStatusChange(event.target.value)}>
            <option value="">Selecione</option>
            {retentionStatusOptions.map((option) => (
              <option key={option.code} value={option.code}>{option.label}</option>
            ))}
          </select>
        </label>
        <label>
          Retencao em dias
          <input
            value={retentionDays}
            onChange={(event) => onRetentionDaysChange(event.target.value)}
            disabled={retentionPolicyStatus !== "configured"}
            inputMode="numeric"
          />
        </label>
        <label>
          Access review
          <select value={accessReviewStatus} onChange={(event) => onAccessReviewStatusChange(event.target.value)}>
            <option value="">Selecione</option>
            {accessReviewStatusOptions.map((option) => (
              <option key={option.code} value={option.code}>{option.label}</option>
            ))}
          </select>
        </label>
        <label>
          Frequencia do review em dias
          <input
            value={accessReviewFrequencyDays}
            onChange={(event) => onAccessReviewFrequencyDaysChange(event.target.value)}
            disabled={accessReviewStatus !== "configured"}
            inputMode="numeric"
          />
        </label>
        <label>
          Versao dos termos
          <input value={termsVersion} onChange={(event) => onTermsVersionChange(event.target.value)} placeholder="Ex.: v2026.03" />
        </label>
        <label>
          <input
            type="checkbox"
            checked={consentTrackingEnabled}
            onChange={(event) => onConsentTrackingEnabledChange(event.target.checked)}
          />
          Rastrear consentimento do workspace
        </label>
        <div className="field-inline" style={{ gridColumn: "1 / -1" }}>
          <button
            className="button-primary"
            type="submit"
            disabled={
              isSaving ||
              retentionStatusOptions.length === 0 ||
              accessReviewStatusOptions.length === 0 ||
              !retentionPolicyStatus ||
              !accessReviewStatus
            }
          >
            {isSaving ? "Salvando controles..." : "Salvar controles"}
          </button>
        </div>
      </form>
      <p className="muted" style={{ marginTop: "0.75rem" }}>
        Retencao {compliance.retentionPolicyStatus}
        {compliance.retentionDays ? ` · ${compliance.retentionDays} dias` : ""}
        {" · "}access review {compliance.accessReviewStatus}
        {compliance.accessReviewFrequencyDays ? ` · ${compliance.accessReviewFrequencyDays} dias` : ""}
      </p>
      <p className="muted">{compliance.note}</p>
      {feedback ? <p className="message-info">{feedback}</p> : null}
    </Panel>
  );
}
