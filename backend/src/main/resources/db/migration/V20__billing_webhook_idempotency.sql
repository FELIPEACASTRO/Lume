ALTER TABLE workspace_payment_events
    ADD CONSTRAINT uk_workspace_payment_event_gateway
        UNIQUE (workspace_id, gateway_event_id);

CREATE INDEX idx_workspace_payment_events_gateway
    ON workspace_payment_events (workspace_id, gateway_event_id);
