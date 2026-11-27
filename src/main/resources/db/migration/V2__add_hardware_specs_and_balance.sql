-- V2__add_hardware_specs_and_balance.sql
-- Вторая версия схемы: добавление сетевых характеристик ПК, спеков и баланса игроков
-- с сохранением ранее накопленных данных

ALTER TABLE computers ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45);
ALTER TABLE computers ADD COLUMN IF NOT EXISTS specs VARCHAR(255);

ALTER TABLE players ADD COLUMN IF NOT EXISTS balance DOUBLE PRECISION NOT NULL DEFAULT 0.0;

-- Содержательное наполнение существующих данных (сохранение накопленных данных)
UPDATE computers
SET ip_address = '192.168.1.' || (100 + id),
    specs = CASE 
        WHEN type = 'VIP' THEN 'RTX 4090, 240Hz, Core i9'
        ELSE 'RTX 4060, 144Hz, Core i5'
    END
WHERE ip_address IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_computers_ip_address ON computers(ip_address);
