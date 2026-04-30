ALTER TABLE trade_items ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity >= 1);

ALTER TABLE user_cards DROP CONSTRAINT IF EXISTS user_cards_quantity_check;
ALTER TABLE user_cards ADD CONSTRAINT user_cards_quantity_check CHECK (quantity >= 1);
