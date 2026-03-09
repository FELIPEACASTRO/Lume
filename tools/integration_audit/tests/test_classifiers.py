from __future__ import annotations

import unittest

from tools.integration_audit.evidence_classifier import build_service_record, classify_coverage
from tools.integration_audit.models import CoverageType, RepoEvidence
from tools.integration_audit.readiness_classifier import apply_readiness


class ClassifiersTest(unittest.TestCase):
    def test_native_implemented_when_backend_and_governance_live(self) -> None:
        evidence = RepoEvidence(
            service="OpenAI",
            canonical_key="openai",
            governance={"implementationStatus": "live", "evidenceLevel": "integration_verified"},
            backend_runtime_files=["backend/src/main/java/com/lume/workspace/inference/adapter/OpenAiAdapter.java"],
            test_files=["backend/src/test/java/OpenAiAdapterTest.java"],
            config_files=[".env.example"],
            env_variables=["OPENAI_API_KEY"],
        )
        self.assertEqual(CoverageType.NATIVE_IMPLEMENTED, classify_coverage(evidence))
        record = apply_readiness(build_service_record("OpenAI", ["OpenAI"], "CHAT", ["CHAT"], evidence), evidence)
        self.assertEqual("READY_IF_SECRETS_PRESENT", record.integratedTestStatus)

    def test_indirect_via_gateway_when_only_gateway_mentions_exist(self) -> None:
        evidence = RepoEvidence(
            service="Portkey",
            canonical_key="portkey",
            gateway_mentions=["docs/ai-providers.md: gateway"],
            docs_files=["docs/ai-providers.md"],
        )
        self.assertEqual(CoverageType.INDIRECT_VIA_GATEWAY, classify_coverage(evidence))

    def test_blocked_threat_intel_becomes_feature_flag_blocker(self) -> None:
        evidence = RepoEvidence(
            service="DarkOwl",
            canonical_key="darkowl",
            governance={"implementationStatus": "blocked", "evidenceLevel": "offline_verified"},
            docs_files=["docs/provider-status-matrix.md"],
            feature_flags=["security.compliance.dark-web-enabled"],
        )
        record = apply_readiness(build_service_record("DarkOwl", ["DarkOwl"], "THREAT_INTEL_SEARCH", ["THREAT_INTEL_SEARCH"], evidence), evidence)
        self.assertEqual("BLOCKED_BY_FEATURE_FLAG", record.integratedTestStatus)
