from __future__ import annotations

import csv
from collections import Counter
from pathlib import Path

from .models import AuditBundle, ServiceAuditRecord, as_markdown_bullets


CSV_HEADERS = [
    "service",
    "aliases",
    "category",
    "capabilities",
    "implementationStatus",
    "integratedTestStatus",
    "evidenceLevel",
    "exposure",
    "coverageType",
    "gatewayProvider",
    "evidenceFiles",
    "keySymbolsOrRoutes",
    "primaryBlocker",
    "notes",
]


def _write_csv(path: Path, records: list[ServiceAuditRecord]) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=CSV_HEADERS)
        writer.writeheader()
        for record in records:
            writer.writerow(record.to_csv_row())


def _records_with(predicate, records: list[ServiceAuditRecord]) -> list[ServiceAuditRecord]:
    return [record for record in records if predicate(record)]


def _write_markdown(path: Path, bundle: AuditBundle) -> None:
    records = bundle.records
    by_status = Counter(record.implementationStatus for record in records)
    by_test_status = Counter(record.integratedTestStatus for record in records)
    ui_without_backend = _records_with(
        lambda item: "frontend" in item.exposure and item.coverageType in {"NOT_IMPLEMENTED", "PARTIAL"},
        records,
    )
    blocked = sorted(
        [record for record in records if record.primaryBlocker != "nenhum"],
        key=lambda item: (item.implementationStatus, item.integratedTestStatus, item.service.casefold()),
    )[:20]

    content = f"""# Auditoria de Integracoes de IA

## 1. Veredito executivo

- Total de servicos auditados: {len(records)}
- Estados de implementacao:
{as_markdown_bullets([f"{key}: {value}" for key, value in sorted(by_status.items())])}
- Estados de readiness integrada:
{as_markdown_bullets([f"{key}: {value}" for key, value in sorted(by_test_status.items())])}

## 2. Mapa da arquitetura encontrada

- A auditoria privilegia codigo executavel e testes antes de metadata e docs.
- `provider-governance-metadata.json` foi tratado como overlay operacional principal.
- O scanner foi executado em modo read-only sobre backend, frontend, docs, config e testes.

## 3. Matriz completa por servico

| Servico | Categoria | Implementacao | Readiness | Cobertura | Evidencia |
|---|---|---|---|---|---|
{chr(10).join(f"| {record.service} | {record.category} | {record.implementationStatus} | {record.integratedTestStatus} | {record.coverageType} | {record.evidenceLevel} |" for record in records)}

## 4. Servicos implementados mas nao testaveis imediatamente

{as_markdown_bullets([f"{record.service}: {record.primaryBlocker}" for record in records if record.implementationStatus in {'live', 'implemented_with_restrictions'} and record.integratedTestStatus not in {'READY_FOR_INTEGRATED_TEST', 'READY_IF_SECRETS_PRESENT'}])}

## 5. Servicos expostos na UI/docs sem runtime comprovado

{as_markdown_bullets([f"{record.service}: {record.coverageType}" for record in ui_without_backend])}

## 6. Top 20 bloqueadores

{as_markdown_bullets([f"{record.service}: {record.primaryBlocker}" for record in blocked])}

## 7. Providers do overlay sem correspondencia canonica no catalogo

{as_markdown_bullets(bundle.unmatched_governance_keys)}

## 8. Safe checks anexados

{as_markdown_bullets([f"`{check.command}` em `{check.workdir}` -> exit {check.exit_code}" for check in bundle.safe_checks], fallback="nenhum safe check executado")}
"""
    path.write_text(content, encoding="utf-8")


def _write_gaps(path: Path, records: list[ServiceAuditRecord]) -> None:
    categories = [
        "CHAT",
        "RESPONSES",
        "EMBEDDINGS",
        "RERANK",
        "WEB_SEARCH",
        "WEB_GROUNDED_CHAT",
        "OCR",
        "SPEECH_TO_TEXT",
        "TEXT_TO_SPEECH",
        "IMAGE_GENERATION",
        "IMAGE_EDITING",
        "VIDEO_GENERATION",
        "THREAT_INTEL_SEARCH",
        "gateway",
        "workflow",
    ]
    lines = ["# Gaps priorizados de integracoes de IA", ""]
    for category in categories:
        relevant = [record for record in records if record.category == category and record.implementationStatus != "live"]
        if not relevant:
            continue
        lines.append(f"## {category}")
        for record in relevant[:20]:
            lines.append(f"- {record.service}: {record.implementationStatus} / {record.integratedTestStatus} / {record.primaryBlocker}")
        lines.append("")
    path.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")


def write_reports(output_dir: str | Path, bundle: AuditBundle) -> dict[str, Path]:
    directory = Path(output_dir)
    directory.mkdir(parents=True, exist_ok=True)
    markdown_path = directory / "auditoria_integracoes_ia.md"
    csv_path = directory / "matriz_integracoes_ia.csv"
    gaps_path = directory / "gaps_priorizados_integracoes_ia.md"
    _write_markdown(markdown_path, bundle)
    _write_csv(csv_path, bundle.records)
    _write_gaps(gaps_path, bundle.records)
    return {
        "markdown": markdown_path,
        "csv": csv_path,
        "gaps": gaps_path,
    }
