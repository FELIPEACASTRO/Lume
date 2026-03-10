# FinOps

## Principios

- custo, quota, tier e rate limit ficam em metadata externa
- routing pode privilegiar `cost-first`, `latency-first` ou `quality-first`
- custo deve ser agregado por workspace, provider e capability
- jobs caros de media e threat-intel exigem feature flag e limite operacional

## Leituras obrigatorias no produto

- custo estimado por chamada quando disponivel
- uso por workspace/provider/capability
- retries, timeouts e fallback como sinal de custo indireto

## Regras

- nao hardcodar preco no core
- nao usar README para declarar economia sem medicao
- providers enterprise e paid so sobem com owner tecnico e owner financeiro claros

