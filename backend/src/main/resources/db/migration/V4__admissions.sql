create table admission_application (
    id                     uuid primary key,
    tenant_id              uuid        not null references tenant (id),
    branch_id              uuid        not null references branch (id),
    academic_year_id       uuid references academic_year (id),
    reference              text        not null,
    status                 text        not null,
    source                 text        not null,
    learner_given_name     text        not null,
    learner_family_name    text        not null,
    learner_date_of_birth  date,
    learner_gender         text,
    grade_level            text        not null,
    boarding_requested     boolean     not null default false,
    guardian_given_name    text        not null,
    guardian_family_name   text        not null,
    guardian_phone         text        not null check (guardian_phone ~ '^254[17][0-9]{8}$'),
    guardian_email         text,
    guardian_relationship  text        not null,
    notes                  text,
    assessment_date        date,
    offer_expires_on       date,
    learner_id             uuid references learner (id),
    created_at             timestamptz not null,
    updated_at             timestamptz not null,
    version                bigint      not null default 0,
    constraint uq_application_reference unique (tenant_id, reference)
);
create index ix_application_branch_status on admission_application (tenant_id, branch_id, status);

create table admission_event (
    id               uuid primary key,
    tenant_id        uuid        not null references tenant (id),
    application_id   uuid        not null references admission_application (id) on delete cascade,
    from_status      text,
    to_status        text        not null,
    note             text,
    actor_person_id  uuid references person (id),
    occurred_at      timestamptz not null,
    created_at       timestamptz not null,
    updated_at       timestamptz not null,
    version          bigint      not null default 0
);
create index ix_admission_event_application on admission_event (application_id, occurred_at);
