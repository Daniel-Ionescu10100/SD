-- User Database Initialization Dump
CREATE TABLE IF NOT EXISTS users (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

INSERT INTO users (id, name, username, password, role) VALUES
    (UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), 'System Administrator', 'admin', 'admin123', 'ADMIN'),
    (UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), 'John Doe', 'client1', 'client123', 'CLIENT')
ON DUPLICATE KEY UPDATE name = VALUES(name);
