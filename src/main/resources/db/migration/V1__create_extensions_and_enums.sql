-- ============================================
-- V1: Extensions and Enums
-- ============================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- User roles
CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');

-- Card rarity levels
CREATE TYPE card_rarity AS ENUM ('COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY', 'SECRET');

-- Card physical condition
CREATE TYPE card_condition AS ENUM ('MINT', 'NEAR_MINT', 'EXCELLENT', 'GOOD', 'PLAYED', 'POOR');

-- Trade statuses
CREATE TYPE trade_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'COMPLETED');

-- Marketplace listing statuses
CREATE TYPE listing_status AS ENUM ('ACTIVE', 'SOLD', 'CANCELLED', 'EXPIRED');

-- Transaction types
CREATE TYPE transaction_type AS ENUM ('PURCHASE', 'SALE', 'TRADE');
