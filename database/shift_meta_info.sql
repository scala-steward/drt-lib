CREATE TABLE shift_meta_info
(
    port                          VARCHAR NOT NULL,
    terminal                      VARCHAR NOT NULL,
    shift_assignments_migrated_at TIMESTAMP,
    CONSTRAINT shift_meta_info_pkey PRIMARY KEY (port, terminal)
);