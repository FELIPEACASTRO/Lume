from __future__ import annotations

import re
import unicodedata
from urllib.parse import urlparse

from .models import CanonicalCatalogEntry, CatalogEntry, sort_unique


CANONICAL_NAME_OVERRIDES = {
    "open ai": "openai",
    "google ai studio gemini": "google-gemini",
    "google gemini": "google-gemini",
    "gemini": "google-gemini",
    "anthropic claude": "anthropic",
    "claude anthropic": "anthropic",
    "claude": "anthropic",
    "mistral ai": "mistral",
    "ai21 labs": "ai21",
    "perplexity sonar": "perplexity",
    "black forest labs flux api": "bfl",
    "grok": "xai",
    "grok xai": "xai",
    "x ai": "xai",
    "hugging face": "hugging-face",
    "cloudflare workers ai": "cloudflare-workers-ai",
    "aws bedrock": "aws-bedrock",
    "amazon bedrock": "aws-bedrock",
    "news catcher": "newscatcher",
    "voyage": "voyage-ai",
    "serp api": "serpapi",
    "full hunt": "fullhunt",
    "runwayml": "runway",
    "eleven labs": "elevenlabs",
    "stability ai": "stability-ai",
    "fal ai": "fal-ai",
    "google cloud vertex ai": "vertex-ai",
}

HOSTNAME_OVERRIDES = {
    "platform.openai.com": "openai",
    "openai.com": "openai",
    "ai.google.dev": "google-gemini",
    "aistudio.google.com": "google-gemini",
    "makersuite.google.com": "google-gemini",
    "anthropic.com": "anthropic",
    "x.ai": "xai",
    "perplexity.ai": "perplexity",
    "console.groq.com": "groq",
    "openrouter.ai": "openrouter",
    "together.ai": "together",
    "fireworks.ai": "fireworks",
    "deepinfra.com": "deepinfra",
    "docs.mistral.ai": "mistral",
    "cohere.com": "cohere",
    "studio.ai21.com": "ai21",
    "exa.ai": "exa",
    "newscatcherapi.com": "newscatcher",
    "tavily.com": "tavily",
    "serpapi.com": "serpapi",
    "voyageai.com": "voyage-ai",
    "stability.ai": "stability-ai",
    "fal.ai": "fal-ai",
    "replicate.com": "replicate",
    "deepgram.com": "deepgram",
    "assemblyai.com": "assemblyai",
    "elevenlabs.io": "elevenlabs",
    "beta.elevenlabs.io": "elevenlabs",
    "runwayml.com": "runway",
    "runwayml.dev": "runway",
    "developer.ideogram.ai": "ideogram",
    "api.bfl.ai": "bfl",
    "docs.bfl.ai": "bfl",
    "docs.fullhunt.io": "fullhunt",
    "api.docs.flare.io": "flare",
    "darkowl.com": "darkowl",
    "twingly.com": "twingly",
    "cloudflare.com": "cloudflare-workers-ai",
}


def normalize_text(value: str) -> str:
    ascii_text = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    normalized = re.sub(r"[^a-zA-Z0-9]+", " ", ascii_text.casefold()).strip()
    return re.sub(r"\s+", " ", normalized)


def infer_capabilities(service: str, raw_category: str, url: str) -> list[str]:
    haystack = " ".join((service, raw_category, url)).casefold()
    capabilities: list[str] = []
    if any(term in haystack for term in ("llm", "chat", "reasoning", "assistant", "responses")):
        capabilities.extend(["CHAT", "RESPONSES"])
    if "embedding" in haystack:
        capabilities.append("EMBEDDINGS")
    if "rerank" in haystack:
        capabilities.append("RERANK")
    if any(term in haystack for term in ("ocr", "document parse", "vision")):
        capabilities.append("OCR")
    if any(term in haystack for term in ("stt", "transcri", "speech-to-text", "speech to text")):
        capabilities.append("SPEECH_TO_TEXT")
    if any(term in haystack for term in ("tts", "text-to-speech", "text to speech", "voice synthesis", "speech synthesis")):
        capabilities.append("TEXT_TO_SPEECH")
    if any(term in haystack for term in ("search", "serp", "crawl", "web search")):
        capabilities.append("WEB_SEARCH")
    if any(term in haystack for term in ("grounded", "citation")):
        capabilities.append("WEB_GROUNDED_CHAT")
    if any(term in haystack for term in ("image", "diffusion", "flux", "ideogram")):
        capabilities.append("IMAGE_GENERATION")
    if any(term in haystack for term in ("edit", "inpaint")):
        capabilities.append("IMAGE_EDITING")
    if any(term in haystack for term in ("video", "avatar")):
        capabilities.append("VIDEO_GENERATION")
    if any(term in haystack for term in ("dark web", "threat intel", "threat-intel", "osint", "attack surface", "leak monitoring", "darknet")):
        capabilities.append("THREAT_INTEL_SEARCH")
    if not capabilities:
        capabilities.append("gateway" if "gateway" in haystack else "workflow")
    return sort_unique(capabilities)


def infer_category(raw_category: str, capabilities: list[str]) -> str:
    if "THREAT_INTEL_SEARCH" in capabilities:
        return "THREAT_INTEL_SEARCH"
    if "WEB_SEARCH" in capabilities and "CHAT" in capabilities:
        return "WEB_GROUNDED_CHAT"
    if capabilities:
        return capabilities[0]
    normalized = normalize_text(raw_category)
    return normalized.replace(" ", "_").upper() if normalized else "workflow"


def canonicalize_service_name(name: str, official_url: str) -> str:
    normalized = normalize_text(name)
    if normalized in CANONICAL_NAME_OVERRIDES:
        return CANONICAL_NAME_OVERRIDES[normalized]
    hostname = urlparse(official_url).netloc.casefold().removeprefix("www.")
    if hostname in HOSTNAME_OVERRIDES:
        return HOSTNAME_OVERRIDES[hostname]
    return normalized.replace(" ", "-")


def normalize_catalog(entries: list[CatalogEntry]) -> list[CanonicalCatalogEntry]:
    by_key: dict[str, CanonicalCatalogEntry] = {}
    alias_to_key: dict[str, str] = {}

    for entry in entries:
        hostname = urlparse(entry.official_url).netloc.casefold().removeprefix("www.")
        key = canonicalize_service_name(entry.service, entry.official_url)
        for alias in entry.aliases + [entry.service]:
            normalized_alias = normalize_text(alias)
            if normalized_alias in alias_to_key:
                key = alias_to_key[normalized_alias]
                break
        capabilities = infer_capabilities(entry.service, entry.raw_category, entry.official_url)
        category = infer_category(entry.raw_category, capabilities)
        current = by_key.get(key)
        if current is None:
            current = CanonicalCatalogEntry(
                service=entry.service,
                canonical_key=key,
                aliases=sort_unique(entry.aliases),
                category=category,
                capabilities=capabilities,
                official_url=entry.official_url,
                official_hostname=hostname,
                source_files=[entry.source_file],
                source_sections=[entry.source_section] if entry.source_section else [],
                raw_categories=[entry.raw_category],
            )
            by_key[key] = current
        else:
            current.aliases = sort_unique(current.aliases + entry.aliases + [entry.service])
            current.capabilities = sort_unique(current.capabilities + capabilities)
            current.source_files = sort_unique(current.source_files + [entry.source_file])
            if entry.source_section:
                current.source_sections = sort_unique(current.source_sections + [entry.source_section])
            current.raw_categories = sort_unique(current.raw_categories + [entry.raw_category])
            if len(entry.official_url) > len(current.official_url):
                current.official_url = entry.official_url
                current.official_hostname = hostname
            if len(entry.service) < len(current.service):
                current.service = entry.service
            if current.category == "workflow" and category != "workflow":
                current.category = category
        for alias in current.aliases + [current.service]:
            alias_to_key[normalize_text(alias)] = key

    return sorted(by_key.values(), key=lambda item: item.service.casefold())
