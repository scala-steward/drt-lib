CREATE TABLE status_daily
(
    port       varchar(3)  NOT NULL,
    terminal   varchar(3)  NOT NULL,
    date_local   varchar(10) NOT NULL,
    pax_loads_updated_at timestamp,
    desk_recommendations_updated_at timestamp,
    staff_deployments_updated_at timestamp,
    staff_updated_at timestamp,
    PRIMARY KEY (port, terminal, date_local)
);

CREATE INDEX status_daily_port_date ON public.status_daily (port, date_local);
