-- ============================================
-- V4: Cards Table
-- ============================================

CREATE TABLE cards (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   
   set_id UUID NOT NULL REFERENCES card_sets(id) ON DELETE CASCADE,
   
   name VARCHAR(200) NOT NULL,
   card_number VARCHAR(50),
   rarity card_rarity NOT NULL,
   
   -- Datos específicos del juego (JSON flexible)
   -- Ejemplo para Pokémon: {"type": "Fire", "hp": 120, "attacks": [...]}
   -- Ejemplo para Magic: {"mana_cost": "{2}{R}", "type": "Creature", "power": 3, "toughness": 3}
   attributes JSONB,
   
   image_url VARCHAR(500),
   image_small_url VARCHAR(500),
   
   market_price DECIMAL(10, 2),
   last_price_update TIMESTAMP,
   
   created_at TIMESTAMP NOT NULL DEFAULT now(),
   updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_cards_set_id ON cards (set_id);
CREATE INDEX idx_cards_name ON cards (name);
CREATE INDEX idx_cards_rarity ON cards (rarity);
CREATE INDEX idx_cards_attributes ON cards USING gin (attributes);
