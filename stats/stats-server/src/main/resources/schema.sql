CREATE TABLE IF NOT EXISTS stats (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app VARCHAR(255) NOT NULL, -- название сервиса, для которого записывается информация
    uri VARCHAR(255) NOT NULL, -- URI, для которого был осуществлён запрос
    ip VARCHAR(15) NOT NULL, -- IP адрес пользователя, осуществившего запрос
    created TIMESTAMP NOT NULL -- дата и время, когда был совершен запрос
);