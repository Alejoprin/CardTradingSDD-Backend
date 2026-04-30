-- ============================================
-- V12: Custom Cards
-- ============================================

CREATE TABLE custom_cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    name VARCHAR(200) NOT NULL,
    card_number VARCHAR(50),
    rarity card_rarity NOT NULL,
    attributes JSONB,

    image_url VARCHAR(500),
    image_small_url VARCHAR(500),
    notes TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_custom_cards_owner_id ON custom_cards (owner_id);

CREATE TRIGGER update_custom_cards_updated_at
BEFORE UPDATE ON custom_cards
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Make card_id nullable and add custom_card_id
ALTER TABLE user_cards ALTER COLUMN card_id DROP NOT NULL;
ALTER TABLE user_cards ADD COLUMN custom_card_id UUID REFERENCES custom_cards(id) ON DELETE CASCADE;

-- Exactly one of card_id or custom_card_id must be set
ALTER TABLE user_cards ADD CONSTRAINT user_cards_card_source_check
    CHECK ((card_id IS NOT NULL)::int + (custom_card_id IS NOT NULL)::int = 1);

CREATE INDEX idx_user_cards_custom_card_id ON user_cards (custom_card_id);
