create table learner (
    id                 uuid primary key,
    tenant_id          uuid        not null references tenant (id),
    branch_id          uuid        not null references branch (id),
    admission_number   text        not null,
    given_name         text        not null,
    family_name        text        not null,
    other_names        text,
    date_of_birth      date,
    gender             text,
    grade_level        text        not null,
    boarding_status    text        not null default 'DAY',
    status             text        not null default 'ACTIVE',
    upi                text,
    assessment_number  text,
    admitted_on        date        not null,
    created_at         timestamptz not null,
    updated_at         timestamptz not null,
    version            bigint      not null default 0,
    constraint uq_learner_admission_number unique (tenant_id, admission_number)
);
create index ix_learner_branch on learner (tenant_id, branch_id, status);
create index ix_learner_name on learner (tenant_id, lower(family_name), lower(given_name));

create table guardian_link (
    id               uuid primary key,
    tenant_id        uuid        not null references tenant (id),
    learner_id       uuid        not null references learner (id) on delete cascade,
    person_id        uuid        not null references person (id),
    relationship     text        not null,
    fee_payer        boolean     not null default false,
    can_pick_up      boolean     not null default true,
    primary_contact  boolean     not null default false,
    created_at       timestamptz not null,
    updated_at       timestamptz not null,
    version          bigint      not null default 0,
    constraint uq_guardian_link unique (learner_id, person_id)
);
create index ix_guardian_link_person on guardian_link (tenant_id, person_id);

create table enrolment (
    id                uuid primary key,
    tenant_id         uuid        not null references tenant (id),
    learner_id        uuid        not null references learner (id) on delete cascade,
    stream_id         uuid        not null references stream (id),
    academic_year_id  uuid        not null references academic_year (id),
    status            text        not null default 'ACTIVE',
    enrolled_on       date        not null,
    created_at        timestamptz not null,
    updated_at        timestamptz not null,
    version           bigint      not null default 0
);
create unique index ux_enrolment_active on enrolment (learner_id, academic_year_id) where status = 'ACTIVE';
create index ix_enrolment_stream on enrolment (stream_id, status);
