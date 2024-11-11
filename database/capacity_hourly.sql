create table "capacity_hourly"
(
    "port"       VARCHAR   NOT NULL,
    "terminal"   VARCHAR   NOT NULL,
    "date_utc"   VARCHAR   NOT NULL,
    "hour"       smallint  NOT NULL,
    "capacity"   smallint  NOT NULL,
    "updated_at" TIMESTAMP NOT NULL
);
alter table "capacity_hourly"
    add constraint "pk_capacity_hourly_port_terminal_dateutc_hour" primary key ("port", "terminal", "date_utc", "hour");
create index "idx_capacity_hourly_date" on "capacity_hourly" ("date_utc");
create index "idx_capacity_hourly_port_date" on "capacity_hourly" ("port", "date_utc");
create index "idx_capacity_hourly_port_terminal_date" on "capacity_hourly" ("port", "terminal", "date_utc");
create index "idx_capacity_hourly_port_terminal_date_hour" on "capacity_hourly" ("port", "terminal", "date_utc", "hour");
