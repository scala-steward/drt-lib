create table "passengers_hourly"
(
    "port"       VARCHAR   NOT NULL,
    "terminal"   VARCHAR   NOT NULL,
    "queue"      VARCHAR   NOT NULL,
    "date_utc"   VARCHAR   NOT NULL,
    "hour"       smallint  NOT NULL,
    "passengers" smallint  NOT NULL,
    "updated_at" TIMESTAMP NOT NULL
);
alter table "passengers_hourly"
    add constraint "pk_passengers_hourly_port_terminal_queue_dateutc_hour" primary key ("port", "terminal", "queue", "date_utc", "hour");
create index "idx_passengers_hourly_date" on "passengers_hourly" ("date_utc");
create index "idx_passengers_hourly_port_date" on "passengers_hourly" ("port", "date_utc");
create index "idx_passengers_hourly_port_terminal_date" on "passengers_hourly" ("port", "terminal", "date_utc");
create index "idx_passengers_hourly_port_terminal_date_hour" on "passengers_hourly" ("port", "terminal", "date_utc", "hour");
