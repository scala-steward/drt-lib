CREATE TABLE passengers_hourly
(
    port       varchar(3)  NOT NULL,
    terminal   varchar(3)  NOT NULL,
    queue      varchar(3)  NOT NULL,
    date_utc   varchar(10) NOT NULL,
    hour       smallint    NOT NULL,
    passengers smallint    NOT NULL,
    created_at timestamp   NOT NULL,
    updated_at timestamp   NOT NULL,
    PRIMARY KEY (port, terminal, queue, date_utc, hour)
);

CREATE INDEX port_terminal_queue_date ON public.passengers_hourly (port, terminal, queue, date_utc);
CREATE INDEX port_terminal_date ON public.passengers_hourly (port, terminal, date_utc);
CREATE INDEX port_date ON public.passengers_hourly (port, date_utc);
