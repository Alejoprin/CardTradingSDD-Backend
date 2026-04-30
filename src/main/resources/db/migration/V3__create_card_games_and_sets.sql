-- ============================================
-- V3: Card Games and Sets Tables
-- ============================================

-- Card Games Table
CREATE TABLE card_games (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   
   name VARCHAR(100) NOT NULL UNIQUE,
   slug VARCHAR(100) NOT NULL UNIQUE,
   description TEXT,
   logo_url VARCHAR(500),
   
   is_active BOOLEAN NOT NULL DEFAULT true,
   
   created_at TIMESTAMP NOT NULL DEFAULT now(),
   updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_card_games_slug ON card_games (slug);

-- Card Sets Table
CREATE TABLE card_sets (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   
   game_id UUID NOT NULL REFERENCES card_games(id) ON DELETE CASCADE,
   
   name VARCHAR(200) NOT NULL,
   code VARCHAR(50) NOT NULL,
   release_date DATE,
   total_cards INTEGER,
   symbol_url VARCHAR(500),
   
   created_at TIMESTAMP NOT NULL DEFAULT now(),
   updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_card_sets_game_id ON card_sets (game_id);
CREATE UNIQUE INDEX idx_card_sets_game_code ON card_sets (game_id, code);
