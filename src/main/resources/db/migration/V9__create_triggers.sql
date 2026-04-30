-- ============================================
-- V9: Triggers for automatic updated_at
-- ============================================

-- Función para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = now();
   RETURN NEW;
END;
$$ language 'plpgsql';

-- Aplicar trigger a users
CREATE TRIGGER update_users_updated_at 
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Aplicar trigger a card_games
CREATE TRIGGER update_card_games_updated_at 
BEFORE UPDATE ON card_games
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Aplicar trigger a card_sets
CREATE TRIGGER update_card_sets_updated_at 
BEFORE UPDATE ON card_sets
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Aplicar trigger a cards
CREATE TRIGGER update_cards_updated_at 
BEFORE UPDATE ON cards
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Aplicar trigger a user_cards
CREATE TRIGGER update_user_cards_updated_at 
BEFORE UPDATE ON user_cards
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Aplicar trigger a wishlists
CREATE TRIGGER update_wishlists_updated_at 
BEFORE UPDATE ON wishlists
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Aplicar trigger a trades
CREATE TRIGGER update_trades_updated_at 
BEFORE UPDATE ON trades
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Aplicar trigger a listings
CREATE TRIGGER update_listings_updated_at 
BEFORE UPDATE ON listings
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Aplicar trigger a reviews
CREATE TRIGGER update_reviews_updated_at 
BEFORE UPDATE ON reviews
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
