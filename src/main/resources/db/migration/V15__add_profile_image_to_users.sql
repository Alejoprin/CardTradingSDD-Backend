-- ============================================
-- V15: Add profile image to users
-- ============================================

ALTER TABLE users
    ADD COLUMN profile_image_url VARCHAR(500);
