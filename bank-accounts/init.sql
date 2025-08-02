CREATE TABLE users
(
    id        BIGSERIAL PRIMARY KEY,
    login     VARCHAR(255) NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,
    name      VARCHAR(255) NOT NULL,
    birthdate DATE         NOT NULL,
    role      VARCHAR(255) NOT NULL
);

CREATE TABLE accounts
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT         NOT NULL,
    currency   VARCHAR(3)     NOT NULL,
    balance    DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE (user_id, currency)
);

INSERT INTO users (login, password, name, birthdate, role)
VALUES ('user', '$2a$10$wXPY16G/JEgBmr83E993Pucpj83S1pNqw6uvnilWzYexYS2AtKDJi', 'user name', '2000-01-01', 'USER'),
       ('user2', '$2a$10$A.s/ZPIkfcFJROxHwAYNbOQVGP1dMslrcFs5jWgtanyVJwTO3BWo2', 'Петр Петров', '2000-01-02', 'USER')