CREATE TABLE IF NOT EXISTS statistic
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY
    PRIMARY
    KEY,
    app_name
    VARCHAR
(
    255
) NOT NULL,
    uri VARCHAR
(
    512
) NOT NULL,
    ip VARCHAR
(
    45
) NOT NULL,
    request_date TIMESTAMP
    );