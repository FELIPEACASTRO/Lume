from __future__ import annotations

import unittest

from tools.integration_audit.catalog_normalizer import normalize_catalog
from tools.integration_audit.models import CatalogEntry


class CatalogNormalizerTest(unittest.TestCase):
    def test_merges_duplicates_by_alias_and_url(self) -> None:
        entries = [
            CatalogEntry(
                service="OpenAI",
                aliases=["OpenAI"],
                raw_category="LLM/API",
                official_url="https://platform.openai.com/",
                source_file="base.txt",
            ),
            CatalogEntry(
                service="Open AI",
                aliases=["Open AI"],
                raw_category="Chat API",
                official_url="https://openai.com/",
                source_file="expanded.txt",
            ),
        ]
        canonical = normalize_catalog(entries)
        self.assertEqual(1, len(canonical))
        self.assertEqual("openai", canonical[0].canonical_key)
        self.assertIn("Open AI", canonical[0].aliases)

    def test_inferrs_search_capability(self) -> None:
        entries = [
            CatalogEntry(
                service="Tavily",
                aliases=["Tavily"],
                raw_category="Search API",
                official_url="https://tavily.com/",
                source_file="base.txt",
            )
        ]
        canonical = normalize_catalog(entries)
        self.assertIn("WEB_SEARCH", canonical[0].capabilities)

    def test_does_not_merge_distinct_products_from_same_hostname(self) -> None:
        entries = [
            CatalogEntry(
                service="Google Cloud Vision API",
                aliases=["Google Cloud Vision API"],
                raw_category="Vision",
                official_url="https://cloud.google.com/vision",
                source_file="base.txt",
            ),
            CatalogEntry(
                service="Google Cloud Speech-to-Text",
                aliases=["Google Cloud Speech-to-Text"],
                raw_category="STT",
                official_url="https://cloud.google.com/speech-to-text",
                source_file="base.txt",
            ),
        ]
        canonical = normalize_catalog(entries)
        self.assertEqual(2, len(canonical))

    def test_audio_intelligence_is_not_threat_intel(self) -> None:
        entries = [
            CatalogEntry(
                service="AssemblyAI",
                aliases=["AssemblyAI"],
                raw_category="STT/Audio Intelligence",
                official_url="https://www.assemblyai.com/dashboard",
                source_file="base.txt",
            )
        ]
        canonical = normalize_catalog(entries)
        self.assertIn("SPEECH_TO_TEXT", canonical[0].capabilities)
        self.assertNotIn("THREAT_INTEL_SEARCH", canonical[0].capabilities)
