CREATE TABLE trade_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_id UUID NOT NULL REFERENCES trades(id),
    card_id UUID NOT NULL REFERENCES cards(id),
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    side VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_trade_items_trade_id ON trade_items (trade_id);
CREATE INDEX idx_trade_items_card_id ON trade_items (card_id);
