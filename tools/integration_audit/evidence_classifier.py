from __future__ import annotations

from .models import CoverageType, EvidenceStrength, RepoEvidence, ServiceAuditRecord


def _implementation_status(evidence: RepoEvidence) -> str:
    if evidence.governance and evidence.governance.get("implementationStatus"):
        return str(evidence.governance["implementationStatus"])
    if evidence.has_backend_runtime and evidence.has_config:
        return "implemented_with_restrictions"
    if evidence.has_backend_runtime:
        return "implemented_with_restrictions"
    if evidence.has_docs or evidence.has_frontend or evidence.has_config:
        return "catalog_only"
    return "out_of_scope"


def _evidence_level(evidence: RepoEvidence) -> str:
    if evidence.governance and evidence.governance.get("evidenceLevel"):
        return str(evidence.governance["evidenceLevel"])
    if evidence.has_backend_runtime and evidence.has_tests:
        return EvidenceStrength.HIGH.value
    if evidence.has_backend_runtime or evidence.has_frontend:
        return EvidenceStrength.MEDIUM.value
    return EvidenceStrength.LOW.value


def classify_coverage(evidence: RepoEvidence) -> CoverageType:
    implementation_status = _implementation_status(evidence)
    if implementation_status in {"blocked", "out_of_scope"}:
        return CoverageType.LEGACY_OR_DISABLED
    if evidence.has_backend_runtime and implementation_status == "live":
        return CoverageType.NATIVE_IMPLEMENTED
    if evidence.has_backend_runtime and implementation_status == "implemented_with_restrictions":
        return CoverageType.PARTIAL
    if not evidence.has_backend_runtime and evidence.gateway_mentions:
        return CoverageType.INDIRECT_VIA_GATEWAY
    if not evidence.has_backend_runtime and evidence.has_tests and not (evidence.has_frontend or evidence.has_docs):
        return CoverageType.STUB_OR_MOCK
    if evidence.has_docs or evidence.has_frontend or evidence.has_config or evidence.governance:
        return CoverageType.NOT_IMPLEMENTED
    return CoverageType.NOT_IMPLEMENTED


def _exposure(evidence: RepoEvidence) -> str:
    channels: list[str] = []
    if evidence.has_backend_runtime:
        channels.append("backend")
    if evidence.has_frontend:
        channels.append("frontend")
    if evidence.has_docs:
        channels.append("docs")
    if evidence.has_config:
        channels.append("config")
    if evidence.has_tests:
        channels.append("tests")
    return "+".join(channels) if channels else "none"


def build_service_record(service: str, aliases: list[str], category: str, capabilities: list[str], evidence: RepoEvidence) -> ServiceAuditRecord:
    implementation_status = _implementation_status(evidence)
    coverage_type = classify_coverage(evidence)
    notes = list(evidence.drift_findings)
    if evidence.governance and evidence.governance.get("notes"):
        notes.append(str(evidence.governance["notes"]))
    if evidence.env_variables:
        notes.append(f"env vars referenciadas: {', '.join(evidence.env_variables)}")
    gateway_provider = evidence.gateway_mentions[0] if evidence.gateway_mentions else None
    return ServiceAuditRecord(
        service=service,
        aliases=aliases,
        category=category,
        capabilities=capabilities,
        implementationStatus=implementation_status,
        integratedTestStatus="UNKNOWN",
        evidenceLevel=_evidence_level(evidence),
        exposure=_exposure(evidence),
        coverageType=coverage_type.value,
        gatewayProvider=gateway_provider,
        evidenceFiles=sorted(
            set(
                evidence.backend_runtime_files
                + evidence.backend_controller_files
                + evidence.frontend_files
                + evidence.test_files
                + evidence.docs_files
                + evidence.config_files
            )
        ),
        keySymbolsOrRoutes=evidence.key_symbols_or_routes[:20],
        primaryBlocker="nenhum" if coverage_type == CoverageType.NATIVE_IMPLEMENTED else "validacao adicional necessaria",
        notes=notes,
    )
