-- ============================================
-- V14: Add optional set reference to custom_cards
-- ============================================

ALTER TABLE custom_cards
    ADD COLUMN set_id UUID REFERENCES card_sets(id) ON DELETE SET NULL;

CREATE INDEX idx_custom_cards_set_id ON custom_cards (set_id);
