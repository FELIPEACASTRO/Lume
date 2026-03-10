# 500 TPS Report

## Estado atual

- o repositorio ainda nao possui evidência empírica para declarar `500 TPS` E2E sustentados
- qualquer claim de throughput maximo permanece bloqueado ate benchmark real

## Plano de medicao

### Ferramentas

- `k6` para carga HTTP
- Micrometer/Actuator para métricas
- JFR para CPU, heap, GC e lock contention

### Degraus

1. `50 TPS`
2. `150 TPS`
3. `300 TPS`
4. `500 TPS`

### Criterios

- latência e erro por capability
- saturação por provider
- consumo de memória
- impacto de fallback, retry e bulkhead

## Politica

- sem benchmark registrado, o README nao declara `500 TPS`
- resultado de laboratorio local nao substitui homologacao com quotas reais

