package com.lume.workspace.service;

import com.lume.workspace.dto.FinopsAnomalyResponse;
import com.lume.workspace.repository.WorkspaceCreditLedgerEntryJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class CreditBalanceAnomalyDetector implements FinopsAnomalyDetector {

    private final WorkspaceCreditLedgerEntryJpaRepository creditLedgerRepository;

    public CreditBalanceAnomalyDetector(WorkspaceCreditLedgerEntryJpaRepository creditLedgerRepository) {
        this.creditLedgerRepository = creditLedgerRepository;
    }

    @Override
    public List<FinopsAnomalyResponse> detect(Long workspaceId, LocalDateTime now) {
        List<FinopsAnomalyResponse> anomalies = new ArrayList<>();

        int creditBalance = creditLedgerRepository.sumCreditsByWorkspaceId(workspaceId);
        if (creditBalance < 0) {
            anomalies.add(FinopsAnomalyDetector.anomaly(
                    "credit_ledger_negative_balance",
                    "critical",
                    "Saldo de creditos negativo",
                    "Credit ledger do workspace ficou negativo.",
                    "Executar reconciliacao e bloquear concessoes manuais indevidas.",
                    now
            ));
        } else if (creditBalance == 0) {
            anomalies.add(FinopsAnomalyDetector.anomaly(
                    "credit_ledger_zero_balance",
                    "warning",
                    "Saldo de creditos zerado",
                    "Workspace sem saldo de creditos disponivel no ledger.",
                    "Oferecer pack adicional ou ajuste de assinatura.",
                    now
            ));
        }

        return anomalies;
    }
}
