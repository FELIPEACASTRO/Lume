from __future__ import annotations

import unittest
from pathlib import Path

from tools.integration_audit.catalog_normalizer import normalize_catalog
from tools.integration_audit.models import CatalogEntry
from tools.integration_audit.repo_scanner import scan_repo


REPO_ROOT = Path(__file__).resolve().parents[3]


class RepoScannerSmokeTest(unittest.TestCase):
    def test_known_providers_have_expected_broad_shapes(self) -> None:
        entries = normalize_catalog(
            [
                CatalogEntry("OpenAI", ["OpenAI"], "LLM/API", "https://platform.openai.com/", "fixture"),
                CatalogEntry("Replicate", ["Replicate"], "Image/Video API", "https://replicate.com/", "fixture"),
                CatalogEntry("DarkOwl", ["DarkOwl"], "Threat Intel", "https://www.darkowl.com/", "fixture"),
                CatalogEntry("Cloudflare Workers AI", ["Cloudflare Workers AI"], "Gateway", "https://developers.cloudflare.com/workers-ai/get-started/rest-api/", "fixture"),
            ]
        )
        evidence_list, _ = scan_repo(REPO_ROOT, entries)
        by_key = {evidence.canonical_key: evidence for evidence in evidence_list}
        self.assertTrue(by_key["openai"].has_backend_runtime)
        self.assertEqual("live", by_key["openai"].governance["implementationStatus"])
        self.assertEqual("implemented_with_restrictions", by_key["replicate"].governance["implementationStatus"])
        self.assertEqual("blocked", by_key["darkowl"].governance["implementationStatus"])
        self.assertEqual("catalog_only", by_key["cloudflare-workers-ai"].governance["implementationStatus"])
