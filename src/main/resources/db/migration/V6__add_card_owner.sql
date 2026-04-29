ALTER TABLE cards ADD COLUMN owner_id UUID REFERENCES users(id);

CREATE INDEX idx_cards_owner_id ON cards (owner_id);
