-- Auth Database Initialization Dump
CREATE TABLE IF NOT EXISTS credentials (
    id BINARY(16) NOT NULL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

-- Passwords:
-- admin / admin123
-- client1 / client123
INSERT INTO credentials (id, username, password_hash, role) VALUES
    (UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), 'admin', '$2a$10$7Wl/uGBEAcKyjrSr8n4AUOKG6I2nONx7FDZ.nGZ7ZJ3FXPGeDR9Ty', 'ADMIN'),
    (UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), 'client1', '$2a$10$3fAWb59LKvt.PGGTtFVbFe..Ex4OEgAdQb6177MhSThepQ4Qoi4Ky', 'CLIENT')
ON DUPLICATE KEY UPDATE role = VALUES(role);
