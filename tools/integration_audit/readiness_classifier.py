from __future__ import annotations

from .models import CoverageType, IntegratedTestStatus, RepoEvidence, ServiceAuditRecord


def classify_readiness(record: ServiceAuditRecord, evidence: RepoEvidence) -> IntegratedTestStatus:
    implementation_status = record.implementationStatus
    if implementation_status == "blocked":
        return IntegratedTestStatus.BLOCKED_BY_FEATURE_FLAG
    if implementation_status == "out_of_scope":
        return IntegratedTestStatus.BLOCKED_BY_BACKEND
    if record.category == "THREAT_INTEL_SEARCH" and evidence.feature_flags:
        return IntegratedTestStatus.BLOCKED_BY_FEATURE_FLAG
    if evidence.governance and evidence.governance.get("businessPriority") == "contract_dependent" and implementation_status in {
        "catalog_only",
        "implemented_with_restrictions",
    } and not evidence.has_backend_runtime:
        return IntegratedTestStatus.BLOCKED_BY_INFRA
    coverage = CoverageType(record.coverageType)
    if coverage == CoverageType.NATIVE_IMPLEMENTED:
        if evidence.has_tests:
            return IntegratedTestStatus.READY_IF_SECRETS_PRESENT if evidence.has_config else IntegratedTestStatus.READY_FOR_INTEGRATED_TEST
        return IntegratedTestStatus.BLOCKED_BY_TEST_HARNESS
    if coverage == CoverageType.PARTIAL:
        if not evidence.has_config:
            return IntegratedTestStatus.BLOCKED_BY_CONFIG
        if not evidence.has_tests:
            return IntegratedTestStatus.BLOCKED_BY_TEST_HARNESS
        return IntegratedTestStatus.READY_IF_SECRETS_PRESENT
    if coverage == CoverageType.INDIRECT_VIA_GATEWAY:
        return IntegratedTestStatus.BLOCKED_BY_BACKEND
    if coverage == CoverageType.STUB_OR_MOCK:
        return IntegratedTestStatus.BLOCKED_BY_BACKEND
    if coverage == CoverageType.LEGACY_OR_DISABLED:
        return IntegratedTestStatus.BLOCKED_BY_FEATURE_FLAG if implementation_status == "blocked" else IntegratedTestStatus.BLOCKED_BY_BACKEND
    if evidence.has_docs or evidence.governance:
        return IntegratedTestStatus.BLOCKED_BY_BACKEND
    return IntegratedTestStatus.UNKNOWN


def apply_readiness(record: ServiceAuditRecord, evidence: RepoEvidence) -> ServiceAuditRecord:
    readiness = classify_readiness(record, evidence)
    record.integratedTestStatus = readiness.value
    if readiness == IntegratedTestStatus.READY_IF_SECRETS_PRESENT:
        record.primaryBlocker = "segredos/credenciais ausentes no ambiente de teste"
    elif readiness == IntegratedTestStatus.BLOCKED_BY_CONFIG:
        record.primaryBlocker = "configuracao/env vars nao comprovadas"
    elif readiness == IntegratedTestStatus.BLOCKED_BY_BACKEND:
        record.primaryBlocker = "wiring de backend nao comprovado"
    elif readiness == IntegratedTestStatus.BLOCKED_BY_FEATURE_FLAG:
        record.primaryBlocker = "bloqueado por feature flag/compliance"
    elif readiness == IntegratedTestStatus.BLOCKED_BY_INFRA:
        record.primaryBlocker = "depende de infra/tenant/contrato externo"
    elif readiness == IntegratedTestStatus.BLOCKED_BY_TEST_HARNESS:
        record.primaryBlocker = "sem harness seguro de teste integrado"
    elif readiness == IntegratedTestStatus.READY_FOR_INTEGRATED_TEST:
        record.primaryBlocker = "nenhum"
    return record
