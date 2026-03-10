# Twingly

- estado: `implemented_with_restrictions`
- evidencia: `integration_verified`
- capabilities: `threat-intel-search`
- auth: `TWINGLY_API_KEY`
- api principal: `POST https://data.twingly.net/darkweb/a/search/v1/search`
- headers: `Authorization: apikey <TWINGLY_API_KEY>`, `Content-Type: application/json; charset=utf-8`, `Accept: application/json; charset=utf-8`
- observacao: runtime real inicial sempre admin-only, com compliance flag, justificativa e auditoria
