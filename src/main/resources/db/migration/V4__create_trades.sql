CREATE TABLE trades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    offerer_id UUID NOT NULL REFERENCES users(id),
    receiver_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    accepted_at TIMESTAMP,
    completed_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX idx_trades_idempotency_key ON trades (idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_trades_offerer_id ON trades (offerer_id);
CREATE INDEX idx_trades_receiver_id ON trades (receiver_id);
CREATE INDEX idx_trades_status ON trades (status);
CREATE INDEX idx_trades_created_at ON trades (created_at);
CREATE INDEX idx_trades_deleted_at ON trades (deleted_at);
