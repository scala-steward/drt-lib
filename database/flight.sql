create table "flight"
(
    "port"                VARCHAR   NOT NULL,
    "origin"              VARCHAR   NOT NULL,
    "terminal"            VARCHAR   NOT NULL,
    "voyage_number"       INTEGER   NOT NULL,
    "carrier_code"        VARCHAR   NOT NULL,
    "flight_code_suffix"  VARCHAR,
    "status"              VARCHAR   NOT NULL,
    "scheduled_date_utc"  VARCHAR   NOT NULL,
    "scheduled"           TIMESTAMP NOT NULL,
    "estimated"           TIMESTAMP,
    "actual"              TIMESTAMP,
    "estimated_chox"      TIMESTAMP,
    "actual_chox"         TIMESTAMP,
    "pcp_time"            TIMESTAMP,
    "carrier_scheduled"   TIMESTAMP,
    "scheduled_departure" TIMESTAMP,
    "predictions"         VARCHAR   NOT NULL,
    "gate"                VARCHAR,
    "stand"               VARCHAR,
    "max_pax"             INTEGER,
    "baggage_reclaim_id"  VARCHAR,
    "pax_sources"         bytea     NOT NULL,
    "red_list_pax"        INTEGER,
    "splits"              bytea     NOT NULL,
    "updated_at"          TIMESTAMP NOT NULL
);
alter table "flight"
    add constraint "pk_flight" primary key ("port", "origin", "terminal", "voyage_number", "scheduled");
create index "idx_flight_port_date" on "flight" ("port", "scheduled_date_utc");
create index "idx_flight_port_date_terminal" on "flight" ("port", "scheduled_date_utc", "terminal");
create index "idx_flight_schedule" on "flight" ("scheduled");
