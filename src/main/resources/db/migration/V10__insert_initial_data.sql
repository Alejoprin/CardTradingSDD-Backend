-- ============================================
-- V10: Initial Data
-- ============================================

-- Insertar juegos de cartas principales
INSERT INTO card_games (name, slug, description, is_active) VALUES
('Pokémon TCG', 'pokemon', 'Pokémon Trading Card Game - El juego de cartas coleccionables basado en la franquicia Pokémon', true),
('Magic: The Gathering', 'magic', 'Magic: The Gathering - El primer y más popular juego de cartas coleccionables del mundo', true),
('Yu-Gi-Oh!', 'yugioh', 'Yu-Gi-Oh! Trading Card Game - Basado en el manga y anime del mismo nombre', true);
