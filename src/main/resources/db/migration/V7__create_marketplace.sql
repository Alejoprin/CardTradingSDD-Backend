-- ============================================
-- V7: Marketplace System
-- ============================================

-- Marketplace Listings Table
CREATE TABLE listings (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   
   seller_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   user_card_id UUID NOT NULL REFERENCES user_cards(id) ON DELETE CASCADE,
   
   price DECIMAL(10, 2) NOT NULL CHECK (price > 0),
   status listing_status NOT NULL DEFAULT 'ACTIVE',
   
   title VARCHAR(200),
   description TEXT,
   
   views INTEGER NOT NULL DEFAULT 0,
   
   listed_at TIMESTAMP NOT NULL DEFAULT now(),
   expires_at TIMESTAMP,
   sold_at TIMESTAMP,
   
   created_at TIMESTAMP NOT NULL DEFAULT now(),
   updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_listings_seller_id ON listings (seller_id);
CREATE INDEX idx_listings_user_card_id ON listings (user_card_id);
CREATE INDEX idx_listings_status ON listings (status);
CREATE INDEX idx_listings_price ON listings (price);

-- Transactions Table
CREATE TABLE transactions (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   
   type transaction_type NOT NULL,
   
   buyer_id UUID REFERENCES users(id) ON DELETE SET NULL,
   seller_id UUID REFERENCES users(id) ON DELETE SET NULL,
   
   listing_id UUID REFERENCES listings(id) ON DELETE SET NULL,
   trade_id UUID REFERENCES trades(id) ON DELETE SET NULL,
   
   amount DECIMAL(10, 2),
   
   completed_at TIMESTAMP NOT NULL DEFAULT now(),
   created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_buyer_id ON transactions (buyer_id);
CREATE INDEX idx_transactions_seller_id ON transactions (seller_id);
CREATE INDEX idx_transactions_type ON transactions (type);
CREATE INDEX idx_transactions_completed_at ON transactions (completed_at);
