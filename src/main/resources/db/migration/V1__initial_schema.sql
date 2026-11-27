-- V1__initial_schema.sql
-- Начальная схема реляционной базы данных компьютерного клуба "Zombie Playa"

CREATE TABLE IF NOT EXISTS players (
    id BIGSERIAL PRIMARY KEY,
    nickname VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS computers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    hourly_rate DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL,
    occupied_by_player_id BIGINT,
    CONSTRAINT fk_computers_occupied_by FOREIGN KEY (occupied_by_player_id)
        REFERENCES players(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_computers_status ON computers(status);
CREATE INDEX IF NOT EXISTS idx_computers_type ON computers(type);
CREATE INDEX IF NOT EXISTS idx_computers_player_id ON computers(occupied_by_player_id);
CREATE INDEX IF NOT EXISTS idx_players_type ON players(type);
