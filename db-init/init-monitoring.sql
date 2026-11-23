-- Monitoring Database Initialization
-- Local copy of the devices that already exist in device_db (same UUIDs).

CREATE TABLE IF NOT EXISTS monitored_device (
    id              BINARY(16) NOT NULL PRIMARY KEY,
    user_id         BINARY(16),
    max_consumption DOUBLE     NOT NULL
);

INSERT INTO monitored_device (id, user_id, max_consumption) VALUES
    (UUID_TO_BIN('33333333-3333-3333-3333-333333333331'), UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), 450.5),
    (UUID_TO_BIN('33333333-3333-3333-3333-333333333332'), UUID_TO_BIN('22222222-2222-2222-2222-222222222222'), 720.0),
    (UUID_TO_BIN('33333333-3333-3333-3333-333333333333'), NULL, 1200.0)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), max_consumption = VALUES(max_consumption);
