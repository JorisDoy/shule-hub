create table grading_scale (
    id          uuid primary key,
    tenant_id   uuid        not null references tenant (id),
    code        text        not null,
    name        text        not null,
    applies_to  text        not null, -- comma-separated LevelBand values
    created_at  timestamptz not null,
    updated_at  timestamptz not null,
    version     bigint      not null default 0,
    constraint uq_grading_scale unique (tenant_id, code)
);

create table grade_band (
    id                uuid primary key,
    tenant_id         uuid          not null references tenant (id),
    grading_scale_id  uuid          not null references grading_scale (id) on delete cascade,
    code              text          not null,
    label             text          not null,
    min_percent       numeric(5, 2) not null check (min_percent between 0 and 100),
    max_percent       numeric(5, 2) not null check (max_percent between 0 and 100),
    points            integer       not null,
    sort_order        integer       not null,
    descriptor        text,
    created_at        timestamptz   not null,
    updated_at        timestamptz   not null,
    version           bigint        not null default 0,
    constraint uq_grade_band unique (grading_scale_id, code),
    constraint ck_grade_band_range check (max_percent >= min_percent)
);

create table assessment (
    id                    uuid primary key,
    tenant_id             uuid          not null references tenant (id),
    branch_id             uuid          not null references branch (id),
    term_id               uuid          not null references term (id),
    stream_id             uuid          not null references stream (id),
    learning_area_id      uuid          not null references learning_area (id),
    title                 text          not null,
    assessment_type       text          not null,
    max_score             numeric(6, 2) check (max_score is null or max_score > 0),
    weight                numeric(5, 2) not null default 1 check (weight > 0),
    assessed_on           date          not null,
    status                text          not null default 'DRAFT',
    created_by_person_id  uuid references person (id),
    created_at            timestamptz   not null,
    updated_at            timestamptz   not null,
    version               bigint        not null default 0
);
create index ix_assessment_stream_term on assessment (tenant_id, stream_id, term_id);

create table assessment_score (
    id                     uuid primary key,
    tenant_id              uuid          not null references tenant (id),
    assessment_id          uuid          not null references assessment (id) on delete cascade,
    learner_id             uuid          not null references learner (id),
    raw_score              numeric(6, 2) check (raw_score is null or raw_score >= 0),
    rubric_level           text,
    comment                text,
    recorded_by_person_id  uuid references person (id),
    created_at             timestamptz   not null,
    updated_at             timestamptz   not null,
    version                bigint        not null default 0,
    constraint uq_assessment_score unique (assessment_id, learner_id)
);
create index ix_assessment_score_learner on assessment_score (tenant_id, learner_id);

create table report_card (
    id                      uuid primary key,
    tenant_id               uuid        not null references tenant (id),
    branch_id               uuid        not null references branch (id),
    learner_id              uuid        not null references learner (id),
    term_id                 uuid        not null references term (id),
    stream_id               uuid        not null references stream (id),
    status                  text        not null default 'DRAFT',
    class_teacher_comment   text,
    head_comment            text,
    approved_at             timestamptz,
    approved_by_person_id   uuid references person (id),
    released_at             timestamptz,
    released_by_person_id   uuid references person (id),
    created_at              timestamptz not null,
    updated_at              timestamptz not null,
    version                 bigint      not null default 0,
    constraint uq_report_card unique (learner_id, term_id)
);
create index ix_report_card_stream_term on report_card (tenant_id, stream_id, term_id);

create table report_card_area_comment (
    id                uuid primary key,
    tenant_id         uuid        not null references tenant (id),
    report_card_id    uuid        not null references report_card (id) on delete cascade,
    learning_area_id  uuid        not null references learning_area (id),
    comment           text        not null,
    created_at        timestamptz not null,
    updated_at        timestamptz not null,
    version           bigint      not null default 0,
    constraint uq_report_card_area_comment unique (report_card_id, learning_area_id)
);
