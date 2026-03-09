from __future__ import annotations

from dataclasses import asdict, dataclass, field
from enum import Enum
from pathlib import Path
from typing import Any


class CoverageType(str, Enum):
    NATIVE_IMPLEMENTED = "NATIVE_IMPLEMENTED"
    INDIRECT_VIA_GATEWAY = "INDIRECT_VIA_GATEWAY"
    PARTIAL = "PARTIAL"
    STUB_OR_MOCK = "STUB_OR_MOCK"
    NOT_IMPLEMENTED = "NOT_IMPLEMENTED"
    LEGACY_OR_DISABLED = "LEGACY_OR_DISABLED"


class IntegratedTestStatus(str, Enum):
    READY_FOR_INTEGRATED_TEST = "READY_FOR_INTEGRATED_TEST"
    READY_IF_SECRETS_PRESENT = "READY_IF_SECRETS_PRESENT"
    BLOCKED_BY_CONFIG = "BLOCKED_BY_CONFIG"
    BLOCKED_BY_BACKEND = "BLOCKED_BY_BACKEND"
    BLOCKED_BY_FRONTEND = "BLOCKED_BY_FRONTEND"
    BLOCKED_BY_FEATURE_FLAG = "BLOCKED_BY_FEATURE_FLAG"
    BLOCKED_BY_INFRA = "BLOCKED_BY_INFRA"
    BLOCKED_BY_TEST_HARNESS = "BLOCKED_BY_TEST_HARNESS"
    UNKNOWN = "UNKNOWN"


class EvidenceStrength(str, Enum):
    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"


@dataclass(slots=True)
class CatalogEntry:
    service: str
    aliases: list[str]
    raw_category: str
    official_url: str
    source_file: str
    source_section: str | None = None
    raw_line: str = ""
    encoding: str = "utf-8"


@dataclass(slots=True)
class CanonicalCatalogEntry:
    service: str
    canonical_key: str
    aliases: list[str]
    category: str
    capabilities: list[str]
    official_url: str
    official_hostname: str
    source_files: list[str] = field(default_factory=list)
    source_sections: list[str] = field(default_factory=list)
    raw_categories: list[str] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)


@dataclass(slots=True)
class RepoEvidence:
    service: str
    canonical_key: str
    matched_governance_key: str | None = None
    governance: dict[str, Any] | None = None
    backend_runtime_files: list[str] = field(default_factory=list)
    backend_controller_files: list[str] = field(default_factory=list)
    frontend_files: list[str] = field(default_factory=list)
    test_files: list[str] = field(default_factory=list)
    docs_files: list[str] = field(default_factory=list)
    config_files: list[str] = field(default_factory=list)
    ci_files: list[str] = field(default_factory=list)
    evidence_snippets: list[str] = field(default_factory=list)
    key_symbols_or_routes: list[str] = field(default_factory=list)
    env_variables: list[str] = field(default_factory=list)
    feature_flags: list[str] = field(default_factory=list)
    drift_findings: list[str] = field(default_factory=list)
    gateway_mentions: list[str] = field(default_factory=list)

    @property
    def has_backend_runtime(self) -> bool:
        return bool(self.backend_runtime_files or self.backend_controller_files)

    @property
    def has_frontend(self) -> bool:
        return bool(self.frontend_files)

    @property
    def has_tests(self) -> bool:
        return bool(self.test_files)

    @property
    def has_docs(self) -> bool:
        return bool(self.docs_files)

    @property
    def has_config(self) -> bool:
        return bool(self.config_files or self.env_variables)


@dataclass(slots=True)
class ServiceAuditRecord:
    service: str
    aliases: list[str]
    category: str
    capabilities: list[str]
    implementationStatus: str
    integratedTestStatus: str
    evidenceLevel: str
    exposure: str
    coverageType: str
    gatewayProvider: str | None
    evidenceFiles: list[str]
    keySymbolsOrRoutes: list[str]
    primaryBlocker: str
    notes: list[str] = field(default_factory=list)

    def to_csv_row(self) -> dict[str, str]:
        return {
            "service": self.service,
            "aliases": ", ".join(self.aliases),
            "category": self.category,
            "capabilities": ", ".join(self.capabilities),
            "implementationStatus": self.implementationStatus,
            "integratedTestStatus": self.integratedTestStatus,
            "evidenceLevel": self.evidenceLevel,
            "exposure": self.exposure,
            "coverageType": self.coverageType,
            "gatewayProvider": self.gatewayProvider or "",
            "evidenceFiles": "; ".join(self.evidenceFiles),
            "keySymbolsOrRoutes": "; ".join(self.keySymbolsOrRoutes),
            "primaryBlocker": self.primaryBlocker,
            "notes": " | ".join(self.notes),
        }


@dataclass(slots=True)
class SafeCheckResult:
    command: str
    workdir: str
    exit_code: int
    stdout_tail: str
    stderr_tail: str


@dataclass(slots=True)
class AuditBundle:
    records: list[ServiceAuditRecord]
    safe_checks: list[SafeCheckResult] = field(default_factory=list)
    canonical_entries: list[CanonicalCatalogEntry] = field(default_factory=list)
    unmatched_governance_keys: list[str] = field(default_factory=list)


def sort_unique(items: list[str]) -> list[str]:
    seen: set[str] = set()
    ordered: list[str] = []
    for item in items:
        if not item:
            continue
        if item not in seen:
            seen.add(item)
            ordered.append(item)
    return ordered


def display_path(path: str | Path) -> str:
    return str(path).replace("\\", "/")


def as_markdown_bullets(items: list[str], fallback: str = "nenhum") -> str:
    if not items:
        return f"- {fallback}"
    return "\n".join(f"- {item}" for item in items)


def dataclass_to_dict(value: Any) -> dict[str, Any]:
    return asdict(value)
