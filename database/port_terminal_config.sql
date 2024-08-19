CREATE TABLE port_terminal_config
(
    port                   varchar(3) NOT NULL,
    terminal               varchar(3) NOT NULL,
    minimum_rostered_staff int,
    updated_at             timestamp NOT NULL,
    PRIMARY KEY (port, terminal)
);
