"use client";

import { useMemo, useState } from "react";
import { ExecutionSetTarget, ModelResponse, ProviderStatus } from "@/lib/api/types";

type Props = {
  providers: ProviderStatus[];
  modelsByProvider: Record<string, ModelResponse[]>;
  executionSet: ExecutionSetTarget[];
  onAddTarget: (target: ExecutionSetTarget) => void;
  onRemoveTarget: (target: ExecutionSetTarget) => void;
};

export function ExecutionSetPanel({
  providers,
  modelsByProvider,
  executionSet,
  onAddTarget,
  onRemoveTarget
}: Props) {
  const [providerCode, setProviderCode] = useState("");
  const [modelCode, setModelCode] = useState("");

  const eligibleProviders = useMemo(
    () =>
      providers.filter(
        (provider) =>
          provider.executionSupported &&
          provider.configured &&
          provider.category === "text-runtime"
      ),
    [providers]
  );

  const availableModels = useMemo(() => {
    if (!providerCode) return [];
    return (modelsByProvider[providerCode] ?? []).filter((model) => model.enabledForAgents);
  }, [providerCode, modelsByProvider]);

  function addTarget() {
    if (!providerCode || !modelCode) {
      return;
    }
    const model = availableModels.find((entry) => entry.code === modelCode);
    if (!model) {
      return;
    }
    onAddTarget({
      providerCode,
      modelCode,
      versionLabel: model.versionLabel
    });
    setModelCode("");
  }

  return (
    <section className="execution-set-panel">
      <header>
        <h3>Execution Set</h3>
        <p>Todos os modelos abaixo serão executados em paralelo na próxima rodada.</p>
      </header>

      <div className="execution-set-controls">
        <label>
          Provedor
          <select value={providerCode} onChange={(event) => {
            setProviderCode(event.target.value);
            setModelCode("");
          }}>
            <option value="">Selecione</option>
            {eligibleProviders.map((provider) => (
              <option key={provider.providerCode} value={provider.providerCode}>
                {provider.providerName}
              </option>
            ))}
          </select>
        </label>
        <label>
          Modelo
          <select value={modelCode} onChange={(event) => setModelCode(event.target.value)} disabled={!providerCode}>
            <option value="">Selecione</option>
            {availableModels.map((model) => (
              <option key={model.code} value={model.code}>
                {model.label} · {model.versionLabel}
              </option>
            ))}
          </select>
        </label>
        <button
          type="button"
          className="button-primary"
          onClick={addTarget}
          disabled={!providerCode || !modelCode}
        >
          Adicionar
        </button>
      </div>

      <div className="execution-set-list">
        {executionSet.length === 0 ? (
          <p className="muted">Nenhum modelo ativo. Adicione ao menos um para executar.</p>
        ) : (
          executionSet.map((target) => (
            <div key={`${target.providerCode}:${target.modelCode}`} className="execution-set-item">
              <div>
                <strong>{target.providerCode}</strong>
                <p>{target.modelCode} · {target.versionLabel || "versão não informada"}</p>
              </div>
              <button type="button" className="button-secondary" onClick={() => onRemoveTarget(target)}>
                Remover
              </button>
            </div>
          ))
        )}
      </div>
    </section>
  );
}
