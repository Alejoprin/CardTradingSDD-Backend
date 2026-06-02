-- ============================================
-- V16: Ads (Anuncios de venta/trueque)
-- ============================================

CREATE TYPE ad_type AS ENUM ('SELL', 'TRADE');
CREATE TYPE ad_status AS ENUM ('ACTIVE', 'CLOSED');

CREATE TABLE ads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    type ad_type NOT NULL,
    card_id UUID NOT NULL REFERENCES cards(id),
    card_name VARCHAR(200) NOT NULL,
    card_image_url VARCHAR(500),
    price DECIMAL(10, 2),
    description TEXT,
    status ad_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_ads_type_status ON ads(type, status);
CREATE INDEX idx_ads_card_name ON ads(card_name);
CREATE INDEX idx_ads_user_id ON ads(user_id);

CREATE TRIGGER update_ads_updated_at
    BEFORE UPDATE ON ads
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
