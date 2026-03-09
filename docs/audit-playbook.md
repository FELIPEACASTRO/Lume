# Audit Playbook

## Objetivo

Padronizar a auditoria read-only das integracoes multi-IA do Lume sem acoplar essa verificacao ao runtime do produto.

## Fonte de verdade

Ordem de precedencia durante a auditoria:

1. codigo executavel + testes verdes
2. `backend/src/main/resources/provider-governance-metadata.json`
3. docs do repo (`provider-status-matrix`, `provider-docs-matrix`, `requirements-matrix`, `live-validation-matrix`, `README`)
4. catalogos anexos de descoberta
5. documentacao oficial do fornecedor apenas para fechar lacunas

## Como executar

### Auditoria basica

```bash
python tools/integration_audit/run_audit.py
```

### Auditoria com checks seguros

```bash
python tools/integration_audit/run_audit.py --run-safe-checks
```

### Testes da ferramenta

```bash
python -m unittest discover tools/integration_audit/tests -v
```

## Entradas padrao

- `~/Downloads/catalogo_unificado_chat_produtos_ia_mar_2026.txt`
- `~/Downloads/catalogo_expandido_agregador_ia_mar_2026.txt`

## Saidas derivadas

Geradas em `reports/integration-audit/`:

- `auditoria_integracoes_ia.md`
- `matriz_integracoes_ia.csv`
- `gaps_priorizados_integracoes_ia.md`

Esses artefatos sao derivados, regeneraveis e nao devem ser versionados.

## Regras conservadoras

- provider listado em enum ou metadata sem adapter real nao conta como implementado
- UI sem wiring real de backend conta como falso positivo
- env var mencionada apenas em docs nao conta como readiness
- docs oficiais nunca promovem provider para `live` sem evidencias no codigo e testes

## Quando atualizar o overlay

Atualize `provider-governance-metadata.json` somente quando houver uma mudanca real no runtime, cobertura de capability, restricao operacional ou nivel de evidencia.
