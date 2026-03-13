# NFR Baseline do Lume

## Performance
- APIs críticas de workspace: alvo `p95 < 300ms` sem inferência externa
- health, sessão e shell: alvo `p95 < 150ms`
- páginas principais: alvo `TTI < 2s` em ambiente padrão de escritório

## Disponibilidade e confiabilidade
- disponibilidade alvo do produto: `99.9%`
- `RPO`: até `1h`
- `RTO`: até `4h`
- nenhum release com flake conhecido nos gates críticos

## Segurança e governança
- rate limit em login com store centralizado
- auditoria para billing, support, BYOK, shell catalog e compliance
- threat-intel sempre admin-only e condicionado a compliance

## Custo e operação
- custo por run e fallback precisam ser observáveis
- anomalias e reconciliação precisam ser acessíveis via API
- provider só entra como destaque quando readiness e smoke estão coerentes
