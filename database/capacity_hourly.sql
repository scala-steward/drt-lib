CREATE TABLE capacity_hourly
(
    port       varchar(3)  NOT NULL,
    terminal   varchar(3)  NOT NULL,
    date_utc   varchar(10) NOT NULL,
    hour       smallint    NOT NULL,
    capacity smallint    NOT NULL,
    updated_at timestamp   NOT NULL,
    PRIMARY KEY (port, terminal, date_utc, hour)
);

CREATE INDEX capacity_hourly_port_terminal_date_hour ON public.capacity_hourly (port, terminal, date_utc, hour);
CREATE INDEX capacity_hourly_port_terminal_date ON public.capacity_hourly (port, terminal, date_utc);
CREATE INDEX capacity_hourly_port_date ON public.capacity_hourly (port, date_utc);
