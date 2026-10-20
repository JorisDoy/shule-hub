-- Spring Modulith 2.x event publication registry (JDBC, schema v2). Acts as the
-- transactional outbox: an event published in a transaction is stored with it and
-- re-delivered to listeners that did not complete.
create table if not exists event_publication (
    id                     uuid        not null,
    listener_id            text        not null,
    event_type             text        not null,
    serialized_event       text        not null,
    publication_date       timestamptz not null,
    completion_date        timestamptz,
    status                 text,
    completion_attempts    integer,
    last_resubmission_date timestamptz,
    primary key (id)
);
create index if not exists event_publication_serialized_event_hash_idx on event_publication using hash (serialized_event);
create index if not exists event_publication_by_completion_date_idx on event_publication (completion_date);
