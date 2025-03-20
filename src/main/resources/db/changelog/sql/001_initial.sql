
CREATE TABLE IF NOT EXISTS users (
                                     id            UUID PRIMARY KEY,
                                     first_name    VARCHAR(255),
    last_name     VARCHAR(255),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    enabled       BOOLEAN   DEFAULT TRUE,
    token_expired BOOLEAN   DEFAULT FALSE,
    creation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS roles (
                                     id   SERIAL PRIMARY KEY,
                                     name VARCHAR(50) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS users_roles (
                                           user_id UUID NOT NULL,
                                           role_id INT  NOT NULL,
                                           PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS locations (
                                         id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL UNIQUE,
    address       VARCHAR(255),
    creation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );


CREATE TABLE IF NOT EXISTS coworking_types (
                                               id   SERIAL PRIMARY KEY,
                                               name VARCHAR(50) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS coworkings (
                                          id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id        UUID NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    coworking_type_id  INT NOT NULL REFERENCES coworking_types(id) ON DELETE RESTRICT,
    name               VARCHAR(255) NOT NULL,
    creation_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (location_id, coworking_type_id, name)
    );

CREATE TABLE IF NOT EXISTS reservations (
                                            id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    coworking_id  UUID NOT NULL REFERENCES coworkings(id) ON DELETE CASCADE,
    res_date      DATE NOT NULL,
    start_time    TIME NOT NULL,
    end_time      TIME NOT NULL,
    status        VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    creation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (coworking_id, res_date, start_time, end_time)
    );

CREATE TABLE IF NOT EXISTS reservation_logs (
                                                id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID,
    action         TEXT NOT NULL,        -- Тип действия (INSERT, UPDATE, DELETE)
    old_data       JSONB,               -- Старые данные (для UPDATE и DELETE)
    new_data       JSONB,               -- Новые данные (для INSERT и UPDATE)
    action_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE OR REPLACE FUNCTION log_reservation_changes()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO reservation_logs (reservation_id, action, old_data, new_data, action_time)
        VALUES (NEW.id, 'INSERT', NULL, row_to_json(NEW), CURRENT_TIMESTAMP);
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO reservation_logs (reservation_id, action, old_data, new_data, action_time)
        VALUES (NEW.id, 'UPDATE', row_to_json(OLD), row_to_json(NEW), CURRENT_TIMESTAMP);
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO reservation_logs (reservation_id, action, old_data, new_data, action_time)
        VALUES (OLD.id, 'DELETE', row_to_json(OLD), NULL, CURRENT_TIMESTAMP);
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Триггер для вставок
CREATE TRIGGER tr_reservation_insert
    AFTER INSERT ON reservations
    FOR EACH ROW
    EXECUTE PROCEDURE log_reservation_changes();

-- Триггер для обновлений
CREATE TRIGGER tr_reservation_update
    AFTER UPDATE ON reservations
    FOR EACH ROW
    EXECUTE PROCEDURE log_reservation_changes();

-- Триггер для удалений
CREATE TRIGGER tr_reservation_delete
    AFTER DELETE ON reservations
    FOR EACH ROW
    EXECUTE PROCEDURE log_reservation_changes();

------------------------------------------------------------
-- 8) Процедура для обновления статуса бронирования
------------------------------------------------------------
CREATE OR REPLACE PROCEDURE update_reservation_status(res_id UUID, new_status VARCHAR)
LANGUAGE plpgsql
AS $$
BEGIN
UPDATE reservations
SET status = new_status, modified_time = NOW()
WHERE id = res_id;

-- Вставляем запись в лог
INSERT INTO reservation_logs (reservation_id, action)
VALUES (res_id, 'UPDATE_STATUS TO ' || new_status);

-- Если бронирование отменено, здесь можно добавить логику освобождения слота,
-- если бы велась отдельная таблица тайм-слотов. Сейчас она не используется.
END;
$$;

-- Базовые «типы коворкингов»
INSERT INTO coworking_types (name) VALUES ('OPEN_SPACE')
    ON CONFLICT (name) DO NOTHING;
INSERT INTO coworking_types (name) VALUES ('MEETING_ROOM')
    ON CONFLICT (name) DO NOTHING;

-- Инициализация локаций (ранее общежития)
INSERT INTO locations (name, address) VALUES ('Антикафе с котами', 'Проспект Мира, д. 10')
    ON CONFLICT (name) DO NOTHING;
INSERT INTO locations (name, address) VALUES ('Антикафе №1', 'Улица Академика Павлова, д. 15')
    ON CONFLICT (name) DO NOTHING;
INSERT INTO locations (name, address) VALUES ('Локация “Икар”', 'Улица Студенческая, д. 25')
    ON CONFLICT (name) DO NOTHING;
INSERT INTO locations (name, address) VALUES ('Пространство “Альфа”', 'Переулок Технологов, д. 5')
    ON CONFLICT (name) DO NOTHING;

WITH loc_mira AS (
    SELECT id FROM locations WHERE name = 'Антикафе с котами'
),
     loc_pavlova AS (
         SELECT id FROM locations WHERE name = 'Антикафе №1'
     )
INSERT INTO coworkings (location_id, coworking_type_id, name)
VALUES
    ((SELECT id FROM loc_mira), 1, 'Open Space #1'),
    ((SELECT id FROM loc_mira), 2, 'Room with cats #2'),

    ((SELECT id FROM loc_pavlova), 1, 'Room with projector #1'),
    ((SELECT id FROM loc_pavlova), 2, 'Room #2')
ON CONFLICT DO NOTHING;