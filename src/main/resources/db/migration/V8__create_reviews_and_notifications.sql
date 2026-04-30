-- ============================================
-- V8: Reviews and Notifications
-- ============================================

-- Reviews Table
CREATE TABLE reviews (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   
   reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   reviewed_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   
   transaction_id UUID REFERENCES transactions(id) ON DELETE SET NULL,
   trade_id UUID REFERENCES trades(id) ON DELETE SET NULL,
   
   rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
   comment TEXT,
   
   created_at TIMESTAMP NOT NULL DEFAULT now(),
   updated_at TIMESTAMP NOT NULL DEFAULT now(),
   
   UNIQUE(reviewer_id, transaction_id),
   UNIQUE(reviewer_id, trade_id),
   CHECK (transaction_id IS NOT NULL OR trade_id IS NOT NULL)
);

CREATE INDEX idx_reviews_reviewer_id ON reviews (reviewer_id);
CREATE INDEX idx_reviews_reviewed_id ON reviews (reviewed_id);
CREATE INDEX idx_reviews_rating ON reviews (rating);

-- Notifications Table
CREATE TABLE notifications (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   
   user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   
   type VARCHAR(50) NOT NULL,
   title VARCHAR(200) NOT NULL,
   message TEXT,
   
   link VARCHAR(500),
   
   is_read BOOLEAN NOT NULL DEFAULT false,
   read_at TIMESTAMP,
   
   created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_is_read ON notifications (is_read);
CREATE INDEX idx_notifications_created_at ON notifications (created_at);
