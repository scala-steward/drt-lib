create table "queue_slot"
(
    "port"                  VARCHAR        NOT NULL,
    "terminal"              VARCHAR        NOT NULL,
    "queue"                 VARCHAR        NOT NULL,
    "slot_start"            TIMESTAMP      NOT NULL,
    "slot_length_minutes"   INTEGER        NOT NULL,
    "slot_date_utc"         VARCHAR        NOT NULL,
    "pax_load"              NUMERIC(14, 4) NOT NULL,
    "work_load"             NUMERIC(14, 4) NOT NULL,
    "desk_rec"              INTEGER        NOT NULL,
    "wait_time"             INTEGER        NOT NULL,
    "pax_in_queue"          INTEGER,
    "deployed_desks"        INTEGER,
    "deployed_wait"         INTEGER,
    "deployed_pax_in_queue" INTEGER,
    "updated_at"            TIMESTAMP      NOT NULL
);
alter table "queue_slot"
    add constraint "pk_queue_slot" primary key ("port", "terminal", "queue", "slot_start", "slot_length_minutes");
create index "idx_queue_slot_port_date" on "queue_slot" ("port", "slot_date_utc");
create index "idx_queue_slot_port_terminal_date" on "queue_slot" ("port", "terminal", "slot_date_utc");
create index "idx_queue_slot_port_terminal_queue_date" on "queue_slot" ("port", "terminal", "queue", "slot_date_utc");
