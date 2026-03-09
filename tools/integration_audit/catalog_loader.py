from __future__ import annotations

import re
from pathlib import Path

from .models import CatalogEntry

ENCODINGS = ("utf-8-sig", "utf-8", "cp1252", "latin-1")


NUMBERED_LINE_PATTERN = re.compile(r"^\s*(?P<index>\d+)\.\s*(?P<body>.+?)\s*$")
URL_PATTERN = re.compile(r"https?://\S+", re.IGNORECASE)


def read_text_with_fallback(path: Path) -> tuple[str, str]:
    if not path.exists():
        raise FileNotFoundError(f"Catalogo nao encontrado: {path}")
    last_error: Exception | None = None
    for encoding in ENCODINGS:
        try:
            return path.read_text(encoding=encoding), encoding
        except UnicodeDecodeError as exc:
            last_error = exc
    raise UnicodeDecodeError(
        "unknown",
        b"",
        0,
        0,
        f"Nao foi possivel decodificar {path}. Ultimo erro: {last_error}",
    )


def _extract_aliases(name: str) -> list[str]:
    aliases = [name.strip()]
    parentheses = re.findall(r"\(([^)]+)\)", name)
    for chunk in parentheses:
        aliases.extend(part.strip() for part in re.split(r"[/,;]| ou ", chunk) if part.strip())
    for splitter in (" / ", "/", " - ", ", "):
        if splitter in name:
            aliases.extend(part.strip() for part in name.split(splitter) if part.strip())
    return _dedupe(aliases)


def _dedupe(values: list[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for value in values:
        normalized = value.casefold()
        if normalized not in seen:
            seen.add(normalized)
            result.append(value)
    return result


def load_catalog(path: str | Path) -> list[CatalogEntry]:
    catalog_path = Path(path)
    text, encoding = read_text_with_fallback(catalog_path)
    entries: list[CatalogEntry] = []
    current_section: str | None = None

    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line:
            continue
        matched_entry = NUMBERED_LINE_PATTERN.match(line)
        if matched_entry and "|" in matched_entry.group("body"):
            parts = [part.strip() for part in matched_entry.group("body").split("|")]
            name = parts[0].strip()
            category = parts[1].strip() if len(parts) > 1 else "workflow"
            url_match = URL_PATTERN.search(line)
            url = url_match.group(0).rstrip(").,;") if url_match else ""
            entries.append(
                CatalogEntry(
                    service=name,
                    aliases=_extract_aliases(name),
                    raw_category=category,
                    official_url=url,
                    source_file=str(catalog_path),
                    source_section=current_section,
                    raw_line=raw_line.rstrip(),
                    encoding=encoding,
                )
            )
            continue
        if not re.match(r"^\d+\.", line):
            current_section = line[:180]
    return entries


def load_catalogs(paths: list[str | Path]) -> list[CatalogEntry]:
    merged: list[CatalogEntry] = []
    for path in paths:
        merged.extend(load_catalog(path))
    return merged
