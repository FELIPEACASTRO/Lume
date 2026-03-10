from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from tools.integration_audit.models import AuditBundle, ServiceAuditRecord
from tools.integration_audit.report_writer import write_reports


class ReportWriterTest(unittest.TestCase):
    def test_generates_all_output_files(self) -> None:
        bundle = AuditBundle(
            records=[
                ServiceAuditRecord(
                    service="OpenAI",
                    aliases=["OpenAI"],
                    category="CHAT",
                    capabilities=["CHAT", "RESPONSES"],
                    implementationStatus="live",
                    integratedTestStatus="READY_IF_SECRETS_PRESENT",
                    evidenceLevel="integration_verified",
                    exposure="backend+docs+tests",
                    coverageType="NATIVE_IMPLEMENTED",
                    gatewayProvider=None,
                    evidenceFiles=["README.md"],
                    keySymbolsOrRoutes=["POST /api/v1/chat"],
                    primaryBlocker="segredos/credenciais ausentes no ambiente de teste",
                    notes=["env vars referenciadas: OPENAI_API_KEY"],
                )
            ]
        )
        with tempfile.TemporaryDirectory() as tmpdir:
            output = write_reports(tmpdir, bundle)
            markdown = Path(output["markdown"]).read_text(encoding="utf-8")
            csv_text = Path(output["csv"]).read_text(encoding="utf-8")
            gaps = Path(output["gaps"]).read_text(encoding="utf-8")
        self.assertIn("Auditoria de Integracoes de IA", markdown)
        self.assertIn("service,aliases,category", csv_text)
        self.assertIn("Gaps priorizados", gaps)
