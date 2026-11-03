-- Device Database Initialization Dump

-- Shadow copy of users from user-service.
-- The id here MUST match the UUID from the user DB.
CREATE TABLE IF NOT EXISTS persons (
    id       BINARY(16)   NOT NULL PRIMARY KEY,
    name     VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS devices (
    id              BINARY(16)   NOT NULL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     VARCHAR(255),
    max_consumption DOUBLE       NOT NULL,
    user_id         BINARY(16),
    CONSTRAINT fk_device_person FOREIGN KEY (user_id) REFERENCES persons(id) ON DELETE SET NULL
);

-- Seed the two known users as persons (same UUIDs as in the user DB)
INSERT INTO persons (id, name, username) VALUES
    (UUID_TO_BIN('11111111-1111-1111-1111-111111111111'), 'System Administrator', 'admin'),
    (UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), 'John Doe', 'client1')
ON DUPLICATE KEY UPDATE name = VALUES(name), username = VALUES(username);

INSERT INTO devices (id, name, description, max_consumption, user_id) VALUES
    (UUID_TO_BIN('33333333-3333-3333-3333-333333333331'), 'Smart Meter Living Room', 'Monitors HVAC and lighting in main living area', 450.5,  UUID_TO_BIN('22222222-2222-2222-2222-222222222222')),
    (UUID_TO_BIN('33333333-3333-3333-3333-333333333332'), 'Smart Meter Kitchen',     'Monitors heavy appliances and oven',            720.0,  UUID_TO_BIN('22222222-2222-2222-2222-222222222222')),
    (UUID_TO_BIN('33333333-3333-3333-3333-333333333333'), 'Smart Meter Garage',      'Unassigned meter for electric vehicle charger', 1200.0, NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name);
