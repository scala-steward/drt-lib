CREATE TABLE shift_staff_rolling
(
    port               VARCHAR NOT NULL,
    terminal           VARCHAR NOT NULL,
    rolling_start_date DATE,
    rolling_end_date   DATE,
    updated_at         TIMESTAMP,
    triggered_by       TEXT,
    CONSTRAINT shift_staff_rolling_pkey PRIMARY KEY (port, terminal, rolling_start_date)
);
