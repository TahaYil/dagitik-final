CREATE TABLE inventory (
    id           BIGSERIAL PRIMARY KEY,
    product_id   BIGINT      NOT NULL UNIQUE,
    quantity     INT         NOT NULL DEFAULT 0,
    reserved     INT         NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_quantity              CHECK (quantity >= 0),
    CONSTRAINT chk_reserved              CHECK (reserved >= 0),
    CONSTRAINT chk_reserved_lte_quantity CHECK (reserved <= quantity)
);

INSERT INTO inventory (product_id, quantity, reserved) VALUES
    (1, 50,  0),
    (2, 200, 0),
    (3, 75,  0),
    (4, 40,  0),
    (5, 120, 0),
    (6, 30,  0),
    (7, 30,  0),
    (8, 15,  0),
    (9, 80,  0),
    (10,100, 0);
