from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from tools.integration_audit.catalog_loader import load_catalog, read_text_with_fallback


class CatalogLoaderTest(unittest.TestCase):
    def test_reads_utf8_catalog(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            path = Path(tmpdir) / "catalog.txt"
            path.write_text("1. OpenAI | LLM/API | URL de acesso: https://platform.openai.com/\n", encoding="utf-8")
            entries = load_catalog(path)
        self.assertEqual(1, len(entries))
        self.assertEqual("OpenAI", entries[0].service)
        self.assertEqual("https://platform.openai.com/", entries[0].official_url)

    def test_reads_cp1252_catalog(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            path = Path(tmpdir) / "catalogo.txt"
            raw = "Seção com acentuação\n1. Anthropic (Claude) | LLM/API | URL oficial: https://www.anthropic.com/\n"
            path.write_bytes(raw.encode("cp1252"))
            text, encoding = read_text_with_fallback(path)
            entries = load_catalog(path)
        self.assertIn("Seção", text)
        self.assertEqual("cp1252", encoding)
        self.assertEqual("Anthropic (Claude)", entries[0].service)

    def test_missing_file_raises_clear_error(self) -> None:
        with self.assertRaises(FileNotFoundError):
            load_catalog("nao-existe.txt")

    def test_parses_numbered_line_without_consolidated_url(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            path = Path(tmpdir) / "catalogo.txt"
            path.write_text(
                "1. DarkOwl | Threat intel / dark web monitoring | URL de acesso: URL oficial nao consolidada nesta rodada\n",
                encoding="utf-8",
            )
            entries = load_catalog(path)
        self.assertEqual(1, len(entries))
        self.assertEqual("DarkOwl", entries[0].service)
        self.assertEqual("", entries[0].official_url)
