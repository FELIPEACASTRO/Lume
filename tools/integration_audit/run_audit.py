from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

if __package__ in {None, ""}:
    sys.path.append(str(Path(__file__).resolve().parents[2]))
    from tools.integration_audit.catalog_loader import load_catalogs
    from tools.integration_audit.catalog_normalizer import normalize_catalog
    from tools.integration_audit.evidence_classifier import build_service_record
    from tools.integration_audit.models import AuditBundle, SafeCheckResult
    from tools.integration_audit.readiness_classifier import apply_readiness
    from tools.integration_audit.repo_scanner import scan_repo
    from tools.integration_audit.report_writer import write_reports
else:
    from .catalog_loader import load_catalogs
    from .catalog_normalizer import normalize_catalog
    from .evidence_classifier import build_service_record
    from .models import AuditBundle, SafeCheckResult
    from .readiness_classifier import apply_readiness
    from .repo_scanner import scan_repo
    from .report_writer import write_reports


def _default_catalog_path(name: str) -> Path:
    return Path.home() / "Downloads" / name


def _run_safe_checks(repo_root: Path) -> list[SafeCheckResult]:
    commands = [
        ("mvn -q -DskipTests compile", repo_root / "backend"),
    ]
    results: list[SafeCheckResult] = []
    for command, workdir in commands:
        completed = subprocess.run(
            command,
            cwd=workdir,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="ignore",
            check=False,
            shell=True,
        )
        results.append(
            SafeCheckResult(
                command=command,
                workdir=str(workdir),
                exit_code=completed.returncode,
                stdout_tail="\n".join(completed.stdout.splitlines()[-20:]),
                stderr_tail="\n".join(completed.stderr.splitlines()[-20:]),
            )
        )
    return results


def run_audit(repo_root: Path, catalog_paths: list[Path], output_dir: Path, run_safe_checks: bool) -> AuditBundle:
    raw_entries = load_catalogs(catalog_paths)
    canonical_entries = normalize_catalog(raw_entries)
    evidence_list, unmatched_governance = scan_repo(repo_root, canonical_entries)
    records = []
    for entry, evidence in zip(canonical_entries, evidence_list, strict=True):
        record = build_service_record(
            service=entry.service,
            aliases=entry.aliases,
            category=entry.category,
            capabilities=entry.capabilities,
            evidence=evidence,
        )
        records.append(apply_readiness(record, evidence))
    bundle = AuditBundle(
        records=sorted(records, key=lambda item: item.service.casefold()),
        safe_checks=_run_safe_checks(repo_root) if run_safe_checks else [],
        canonical_entries=canonical_entries,
        unmatched_governance_keys=unmatched_governance,
    )
    write_reports(output_dir, bundle)
    return bundle


def main() -> int:
    parser = argparse.ArgumentParser(description="Auditoria read-only das integracoes multi-IA do Lume")
    parser.add_argument("--repo-root", default=".", help="Raiz do repositorio")
    parser.add_argument(
        "--catalog-base",
        default=str(_default_catalog_path("catalogo_unificado_chat_produtos_ia_mar_2026.txt")),
        help="Caminho para o catalogo base",
    )
    parser.add_argument(
        "--catalog-expanded",
        default=str(_default_catalog_path("catalogo_expandido_agregador_ia_mar_2026.txt")),
        help="Caminho para o catalogo incremental/expandido",
    )
    parser.add_argument(
        "--output-dir",
        default="reports/integration-audit",
        help="Diretorio de saida dos relatorios derivados",
    )
    parser.add_argument(
        "--run-safe-checks",
        action="store_true",
        help="Executa compilacao/lint/testes seguros e anexa o resultado ao relatorio",
    )
    args = parser.parse_args()
    bundle = run_audit(
        repo_root=Path(args.repo_root).resolve(),
        catalog_paths=[Path(args.catalog_base), Path(args.catalog_expanded)],
        output_dir=Path(args.output_dir),
        run_safe_checks=args.run_safe_checks,
    )
    print(f"Auditoria concluida. Servicos auditados: {len(bundle.records)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
