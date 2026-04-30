-- ============================================
-- V6: Trades System
-- ============================================

-- Trades Table
CREATE TABLE trades (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   
   proposer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   receiver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   
   status trade_status NOT NULL DEFAULT 'PENDING',
   
   proposer_notes TEXT,
   receiver_notes TEXT,
   
   proposed_at TIMESTAMP NOT NULL DEFAULT now(),
   responded_at TIMESTAMP,
   completed_at TIMESTAMP,
   
   created_at TIMESTAMP NOT NULL DEFAULT now(),
   updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_trades_proposer_id ON trades (proposer_id);
CREATE INDEX idx_trades_receiver_id ON trades (receiver_id);
CREATE INDEX idx_trades_status ON trades (status);

-- Trade Items Table
CREATE TABLE trade_items (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   
   trade_id UUID NOT NULL REFERENCES trades(id) ON DELETE CASCADE,
   user_card_id UUID NOT NULL REFERENCES user_cards(id) ON DELETE CASCADE,
   
   -- De quién es la carta en el intercambio
   from_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   
   created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_trade_items_trade_id ON trade_items (trade_id);
CREATE INDEX idx_trade_items_user_card_id ON trade_items (user_card_id);
