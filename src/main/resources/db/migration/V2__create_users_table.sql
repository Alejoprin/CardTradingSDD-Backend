-- ============================================
-- V2: Users Table
-- ============================================

CREATE TABLE users (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

   username VARCHAR(20) NOT NULL,
   email VARCHAR(255) NOT NULL,
   password VARCHAR(255) NOT NULL,

   role user_role NOT NULL DEFAULT 'USER',

   is_banned BOOLEAN NOT NULL DEFAULT false,

   created_at TIMESTAMP NOT NULL DEFAULT now(),
   updated_at TIMESTAMP NOT NULL DEFAULT now(),
   deleted_at TIMESTAMP
);

-- Indexes
CREATE UNIQUE INDEX idx_users_email ON users (email) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_users_username ON users (username) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_deleted_at ON users (deleted_at);
