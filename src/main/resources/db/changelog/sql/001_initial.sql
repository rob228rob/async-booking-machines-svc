-- 001_init_tables.sql
-- Liquibase formatted SQL file

-- Liquibase ChangeSet
-- Эта миграция предполагает создание следующих таблиц и объектов:
-- 0) users, roles, users_roles - таблички для управления ролями и разграничения прав
-- 1) locations: Список локаций
-- 2) coworking_types: Типы коворкингов (при необходимости)
-- 3) coworkings: Конкретные коворкинги, привязанные к локации
-- 5) reservations: Таблица бронирований коворкингов пользователями с добавлением статуса
-- 6) reservation_logs: Логирование действий с бронированиями (после вставки или обновления статуса)
-- 7) Триггеры и функция для логирования вставок/обновлений/удалений в reservations
-- 8) Процедура для обновления статуса бронирования
-- 9) Инициализация начальных данных для локаций, типов коворкингов и примеров коворкингов

-- changeset admin:001 createTable

------------------------------------------------------------
-- 0) Таблицы пользователей и ролей
------------------------------------------------------------
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

------------------------------------------------------------
-- 1) Таблица локаций (ранее "dormitories")
------------------------------------------------------------
CREATE TABLE IF NOT EXISTS locations (
                                         id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL UNIQUE,
    address       VARCHAR(255),
    creation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

------------------------------------------------------------
-- 2) Таблица типов коворкингов (ранее "machine_types"), при необходимости
------------------------------------------------------------
CREATE TABLE IF NOT EXISTS coworking_types (
                                               id   SERIAL PRIMARY KEY,
                                               name VARCHAR(50) NOT NULL UNIQUE
    );

------------------------------------------------------------
-- 3) Таблица коворкингов (ранее "coworkings")
------------------------------------------------------------
CREATE TABLE IF NOT EXISTS coworkings (
                                          id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id        UUID NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    coworking_type_id  INT NOT NULL REFERENCES coworking_types(id) ON DELETE RESTRICT,
    name               VARCHAR(255) NOT NULL,
    creation_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (location_id, coworking_type_id, name)
    );

------------------------------------------------------------
-- 5) Таблица бронирований (reservations)
------------------------------------------------------------
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

------------------------------------------------------------
-- 6) Таблица логов бронирований (reservation_logs)
------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservation_logs (
                                                id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id UUID,
    action         TEXT NOT NULL,        -- Тип действия (INSERT, UPDATE, DELETE)
    old_data       JSONB,               -- Старые данные (для UPDATE и DELETE)
    new_data       JSONB,               -- Новые данные (для INSERT и UPDATE)
    action_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

------------------------------------------------------------
-- 7) Функция и триггеры для логирования вставок/обновлений/удалений
------------------------------------------------------------
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

------------------------------------------------------------
-- 9) Инициализация данных
--    (ДЛЯ ПРИМЕРА: меняем названия; если нужна другая тестовая инфа — подставьте сами)
------------------------------------------------------------

-- Базовые «типы коворкингов»
INSERT INTO coworking_types (name) VALUES ('OPEN_SPACE')
    ON CONFLICT (name) DO NOTHING;
INSERT INTO coworking_types (name) VALUES ('MEETING_ROOM')
    ON CONFLICT (name) DO NOTHING;

-- Инициализация локаций (ранее общежития)
INSERT INTO locations (name, address) VALUES ('Офис на Мира', 'Проспект Мира, д. 10')
    ON CONFLICT (name) DO NOTHING;
INSERT INTO locations (name, address) VALUES ('Коворкинг на Павлова', 'Улица Академика Павлова, д. 15')
    ON CONFLICT (name) DO NOTHING;
INSERT INTO locations (name, address) VALUES ('Локация “Икар”', 'Улица Студенческая, д. 25')
    ON CONFLICT (name) DO NOTHING;
INSERT INTO locations (name, address) VALUES ('Пространство “Альфа”', 'Переулок Технологов, д. 5')
    ON CONFLICT (name) DO NOTHING;

-- Пример вставки коворкингов
WITH loc_mira AS (
    SELECT id FROM locations WHERE name = 'Офис на Мира'
),
     loc_pavlova AS (
         SELECT id FROM locations WHERE name = 'Коворкинг на Павлова'
     )
INSERT INTO coworkings (location_id, coworking_type_id, name)
VALUES
    -- Локация «Офис на Мира»
    ((SELECT id FROM loc_mira), 1, 'Open Space #1'),
    ((SELECT id FROM loc_mira), 2, 'Meeting Room #2'),

    -- Локация «Коворкинг на Павлова»
    ((SELECT id FROM loc_pavlova), 1, 'Open Space #1'),
    ((SELECT id FROM loc_pavlova), 2, 'Negotiation Room #2')
ON CONFLICT DO NOTHING;