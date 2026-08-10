CREATE TABLE users (
                       id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(60)  NOT NULL,
                       role          VARCHAR(20)  NOT NULL CHECK (role IN ('CUSTOMER','VENDOR','ADMIN')),
                       created_at    TIMESTAMPTZ  NOT NULL,
                       updated_at    TIMESTAMPTZ
);

CREATE TABLE vendors(
                        id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        owner_id   UUID         NOT NULL REFERENCES users(id),
                        name       VARCHAR(255) NOT NULL,
                        address    VARCHAR(255) NOT NULL,
                        status     VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING','APPROVED','SUSPENDED')),
                        created_at TIMESTAMPTZ  NOT NULL,
                        updated_at TIMESTAMPTZ
);

CREATE TABLE menu_items (
                            id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            vendor_id   UUID          NOT NULL REFERENCES vendors(id),
                            name        VARCHAR(255)  NOT NULL,
                            description VARCHAR(255),
                            price       NUMERIC(10,2) NOT NULL,
                            available   BOOLEAN       NOT NULL,
                            category    VARCHAR(255),
                            created_at  TIMESTAMPTZ   NOT NULL,
                            updated_at  TIMESTAMPTZ
);

CREATE TABLE orders(
                       id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       customer_id UUID NOT NULL REFERENCES users(id),
                        vendor_id  UUID NOT NULL REFERENCES vendors(id),
                       status VARCHAR(20) NOT NULL CHECK (status IN (
                                                                     'PLACED','ACCEPTED','PREPARING','READY',
                                                                     'OUT_FOR_DELIVERY','DELIVERED','CANCELLED','REJECTED'
                           )),
                       total NUMERIC(10,2) NOT NULL,
                       created_at  TIMESTAMPTZ   NOT NULL,
                       updated_at  TIMESTAMPTZ

);

CREATE TABLE order_items (
                             id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             order_id     UUID          NOT NULL REFERENCES orders(id),
                             menu_item_id UUID          REFERENCES menu_items(id),
                             item_name    VARCHAR(255)  NOT NULL,
                             unit_price   NUMERIC(10,2) NOT NULL,
                             quantity     INT           NOT NULL,
                             created_at   TIMESTAMPTZ   NOT NULL,
                             updated_at   TIMESTAMPTZ
);