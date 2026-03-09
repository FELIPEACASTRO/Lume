from __future__ import annotations

import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from .catalog_normalizer import normalize_text
from .models import CanonicalCatalogEntry, RepoEvidence, display_path, sort_unique

TEXT_SUFFIXES = {
    ".java",
    ".kt",
    ".py",
    ".ts",
    ".tsx",
    ".js",
    ".jsx",
    ".json",
    ".md",
    ".yml",
    ".yaml",
    ".txt",
    ".xml",
    ".properties",
    ".env",
}

IGNORED_PARTS = {
    ".git",
    "node_modules",
    "target",
    "dist",
    "coverage",
    ".vite",
    "reports",
    "__pycache__",
}

GATEWAY_HINTS = ("gateway", "openrouter", "portkey", "vercel ai gateway", "cloudflare")


@dataclass(slots=True)
class IndexedFile:
    path: Path
    relative_path: str
    content: str
    lowered: str
    normalized: str


def load_governance(repo_root: Path) -> dict[str, dict]:
    governance_path = repo_root / "backend" / "src" / "main" / "resources" / "provider-governance-metadata.json"
    if not governance_path.exists():
        return {}
    payload = json.loads(governance_path.read_text(encoding="utf-8"))
    return payload.get("providers", {})


def _iter_text_files(repo_root: Path) -> Iterable[Path]:
    for path in repo_root.rglob("*"):
        if not path.is_file():
            continue
        if any(part in IGNORED_PARTS for part in path.parts):
            continue
        if path.suffix.casefold() not in TEXT_SUFFIXES and path.name not in {"README.md", ".env.example", "pom.xml", "package.json"}:
            continue
        yield path


def build_file_index(repo_root: Path) -> list[IndexedFile]:
    indexed: list[IndexedFile] = []
    for path in _iter_text_files(repo_root):
        try:
            content = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            content = path.read_text(encoding="utf-8", errors="ignore")
        indexed.append(
            IndexedFile(
                path=path,
                relative_path=display_path(path.relative_to(repo_root)),
                content=content,
                lowered=content.casefold(),
                normalized=normalize_text(content),
            )
        )
    return indexed


def _match_alias(normalized_content: str, alias: str) -> bool:
    alias = alias.strip()
    if not alias:
        return False
    normalized = normalize_text(alias)
    if not normalized:
        return False
    token_pattern = re.escape(normalized).replace(r"\ ", r"[\s_\-]+")
    return re.search(rf"(?<![a-z0-9]){token_pattern}(?![a-z0-9])", normalized_content) is not None


def _provider_aliases(entry: CanonicalCatalogEntry) -> list[str]:
    aliases = [entry.service, entry.canonical_key]
    aliases.extend(entry.aliases)
    if entry.canonical_key == "google-gemini":
        aliases.extend(["gemini", "google gemini"])
    if entry.canonical_key == "anthropic":
        aliases.extend(["claude", "anthropic"])
    if entry.canonical_key == "xai":
        aliases.extend(["grok", "xai", "x ai"])
    return sort_unique([alias for alias in aliases if alias])


def _collect_env_variables(indexed_file: IndexedFile, service_aliases: list[str]) -> list[str]:
    envs: list[str] = []
    for line in indexed_file.content.splitlines():
        if "=" not in line:
            continue
        key = line.split("=", 1)[0].strip()
        if not key:
            continue
        lowered_line = line.casefold()
        if any(alias.casefold() in lowered_line for alias in service_aliases):
            envs.append(key)
    return sort_unique(envs)


def _classify_path(relative_path: str) -> str:
    path = relative_path.casefold()
    if path.startswith("backend/src/main/java/"):
        if "/controller/" in path:
            return "backend_controller"
        return "backend_runtime"
    if "/src/test/" in path or path.startswith("frontend/tests/") or path.startswith("tools/integration_audit/tests/"):
        return "test"
    if path.startswith("frontend/src/"):
        return "frontend"
    if path.startswith("docs/") or path == "readme.md":
        return "docs"
    if any(name in path for name in ("package.json", "pom.xml", ".github/", "docker-compose", ".env.example")):
        return "config"
    return "other"


def scan_repo(repo_root: str | Path, entries: list[CanonicalCatalogEntry]) -> tuple[list[RepoEvidence], list[str]]:
    root = Path(repo_root)
    governance = load_governance(root)
    indexed_files = build_file_index(root)
    unmatched_governance_keys = set(governance.keys())
    records: list[RepoEvidence] = []

    for entry in entries:
        aliases = _provider_aliases(entry)
        normalized_candidates = {normalize_text(alias) for alias in aliases}
        matched_governance_key = next(
            (
                key
                for key in governance.keys()
                if key == entry.canonical_key
                or normalize_text(key) in normalized_candidates
                or normalize_text(key.replace("-", " ")) in normalized_candidates
            ),
            None,
        )
        evidence = RepoEvidence(
            service=entry.service,
            canonical_key=entry.canonical_key,
            matched_governance_key=matched_governance_key,
            governance=governance.get(matched_governance_key) if matched_governance_key else None,
        )
        if matched_governance_key:
            unmatched_governance_keys.discard(matched_governance_key)

        official_host = entry.official_hostname.casefold()
        for indexed in indexed_files:
            matched = False
            if official_host and official_host in indexed.lowered:
                matched = True
            elif any(_match_alias(indexed.normalized, alias) for alias in aliases):
                matched = True
            if not matched:
                continue
            bucket = _classify_path(indexed.relative_path)
            relative_path = indexed.relative_path
            if bucket == "backend_runtime":
                evidence.backend_runtime_files.append(relative_path)
            elif bucket == "backend_controller":
                evidence.backend_controller_files.append(relative_path)
            elif bucket == "frontend":
                evidence.frontend_files.append(relative_path)
            elif bucket == "test":
                evidence.test_files.append(relative_path)
            elif bucket == "docs":
                evidence.docs_files.append(relative_path)
            elif bucket == "config":
                evidence.config_files.append(relative_path)
            else:
                evidence.ci_files.append(relative_path)

            if indexed.relative_path.endswith(".env.example"):
                evidence.env_variables.extend(_collect_env_variables(indexed, aliases))
            for line in indexed.content.splitlines():
                lowered_line = line.casefold()
                if official_host and official_host in lowered_line or any(alias.casefold() in lowered_line for alias in aliases):
                    clean = line.strip()
                    if clean:
                        if any(token in clean for token in ("@GetMapping", "@PostMapping", "@PatchMapping", "api/v1/", "GET /api/", "POST /api/")):
                            evidence.key_symbols_or_routes.append(clean[:220])
                        if any(flag in lowered_line for flag in ("dark-web-enabled", "feature", "flag", "compliance")):
                            evidence.feature_flags.append(clean[:220])
                        if any(hint in lowered_line for hint in GATEWAY_HINTS):
                            evidence.gateway_mentions.append(clean[:220])
                        if "class " in clean or "interface " in clean or "function " in clean or "const " in clean:
                            evidence.key_symbols_or_routes.append(clean[:220])
                        evidence.evidence_snippets.append(f"{relative_path}: {clean[:220]}")
                        if len(evidence.evidence_snippets) >= 25:
                            break
        if evidence.governance and evidence.governance.get("implementationStatus") == "live" and not evidence.has_backend_runtime:
            evidence.drift_findings.append("governanca marca live, mas nao foi encontrada evidencia forte de runtime no backend")
        if evidence.has_frontend and not evidence.has_backend_runtime:
            evidence.drift_findings.append("ha exposicao em UI/docs sem wiring claro de backend")
        if evidence.governance is None and (evidence.has_backend_runtime or evidence.has_frontend or evidence.has_docs):
            evidence.drift_findings.append("ha evidencia no repo, mas o provider nao esta no overlay oficial de governanca")

        evidence.backend_runtime_files = sort_unique(evidence.backend_runtime_files)
        evidence.backend_controller_files = sort_unique(evidence.backend_controller_files)
        evidence.frontend_files = sort_unique(evidence.frontend_files)
        evidence.test_files = sort_unique(evidence.test_files)
        evidence.docs_files = sort_unique(evidence.docs_files)
        evidence.config_files = sort_unique(evidence.config_files)
        evidence.ci_files = sort_unique(evidence.ci_files)
        evidence.evidence_snippets = sort_unique(evidence.evidence_snippets)
        evidence.key_symbols_or_routes = sort_unique(evidence.key_symbols_or_routes)
        evidence.env_variables = sort_unique(evidence.env_variables)
        evidence.feature_flags = sort_unique(evidence.feature_flags)
        evidence.gateway_mentions = sort_unique(evidence.gateway_mentions)
        records.append(evidence)

    return records, sorted(unmatched_governance_keys)
