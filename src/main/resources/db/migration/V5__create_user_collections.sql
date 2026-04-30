-- ============================================
-- V5: User Collections Tables
-- ============================================

-- User Cards Table (Collection)
CREATE TABLE user_cards (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   
   user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   card_id UUID NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
   
   quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity >= 0),
   condition card_condition NOT NULL DEFAULT 'NEAR_MINT',
   
   is_for_trade BOOLEAN NOT NULL DEFAULT false,
   is_for_sale BOOLEAN NOT NULL DEFAULT false,
   
   notes TEXT,
   
   acquired_at TIMESTAMP NOT NULL DEFAULT now(),
   created_at TIMESTAMP NOT NULL DEFAULT now(),
   updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_cards_user_id ON user_cards (user_id);
CREATE INDEX idx_user_cards_card_id ON user_cards (card_id);
CREATE INDEX idx_user_cards_for_trade ON user_cards (is_for_trade) WHERE is_for_trade = true;
CREATE INDEX idx_user_cards_for_sale ON user_cards (is_for_sale) WHERE is_for_sale = true;

-- Wishlists Table
CREATE TABLE wishlists (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   
   user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   card_id UUID NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
   
   priority INTEGER DEFAULT 0,
   max_price DECIMAL(10, 2),
   notes TEXT,
   
   created_at TIMESTAMP NOT NULL DEFAULT now(),
   updated_at TIMESTAMP NOT NULL DEFAULT now(),
   
   UNIQUE(user_id, card_id)
);

CREATE INDEX idx_wishlists_user_id ON wishlists (user_id);
CREATE INDEX idx_wishlists_card_id ON wishlists (card_id);
