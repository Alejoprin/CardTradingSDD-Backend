CREATE TABLE cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    rarity VARCHAR(20) NOT NULL,
    edition VARCHAR(100),
    image_url VARCHAR(500),
    card_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX idx_cards_name ON cards (name) WHERE deleted_at IS NULL;
CREATE INDEX idx_cards_rarity ON cards (rarity);
CREATE INDEX idx_cards_card_type ON cards (card_type);
CREATE INDEX idx_cards_deleted_at ON cards (deleted_at);
