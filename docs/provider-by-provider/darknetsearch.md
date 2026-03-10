# DarknetSearch

- estado: `implemented_with_restrictions`
- evidencia: `integration_verified`
- capabilities: `threat-intel-search`
- auth: `DARKNETSEARCH_USERNAME`, `DARKNETSEARCH_PASSWORD`, `DARKNETSEARCH_CLIENT_ID`, `DARKNETSEARCH_CLIENT_SECRET`
- api principal: `GET https://client-api.leak.center/api/service/leak_extended_database_search/`
- token: `POST https://app.leak.center/uaa/oauth/token` via password flow com client auth basica
- observacao: runtime real inicial sempre admin-only, com compliance flag, justificativa e auditoria
