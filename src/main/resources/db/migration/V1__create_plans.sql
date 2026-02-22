CREATE TABLE plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    monthly_limit INT NOT NULL,
    requests_per_minute INT NOT NULL,
    requests_per_day INT NOT NULL,
    max_api_keys INT NOT NULL,
    ml_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    custom_threshold BOOLEAN NOT NULL DEFAULT FALSE,
    webhook_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    false_positive_reporting BOOLEAN NOT NULL DEFAULT FALSE,
    price_monthly_pence INT NOT NULL DEFAULT 0,
    price_yearly_pence INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO plans (name, monthly_limit, requests_per_minute, requests_per_day, max_api_keys,
                   ml_enabled, custom_threshold, webhook_enabled, false_positive_reporting,
                   price_monthly_pence, price_yearly_pence)
VALUES
    ('free',     100,       10,  100,  1, FALSE, FALSE, FALSE, FALSE,    0,    0),
    ('pro',      5000,      60, 5000,  5, TRUE,  TRUE,  TRUE,  TRUE,   700, 6000),
    ('business', 999999999, 300,  -1, 20, TRUE,  TRUE,  TRUE,  TRUE,  1800, 15000);
