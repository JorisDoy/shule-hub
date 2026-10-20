create table academic_year (
    id          uuid primary key,
    tenant_id   uuid        not null references tenant (id),
    year        integer     not null,
    start_date  date        not null,
    end_date    date        not null,
    created_at  timestamptz not null,
    updated_at  timestamptz not null,
    version     bigint      not null default 0,
    constraint uq_academic_year unique (tenant_id, year),
    constraint ck_academic_year_dates check (end_date > start_date)
);

create table term (
    id                uuid primary key,
    tenant_id         uuid        not null references tenant (id),
    academic_year_id  uuid        not null references academic_year (id) on delete cascade,
    number            integer     not null check (number between 1 and 3),
    start_date        date        not null,
    end_date          date        not null,
    created_at        timestamptz not null,
    updated_at        timestamptz not null,
    version           bigint      not null default 0,
    constraint uq_term unique (academic_year_id, number),
    constraint ck_term_dates check (end_date > start_date)
);
create index ix_term_tenant on term (tenant_id);

create table stream (
    id                      uuid primary key,
    tenant_id               uuid        not null references tenant (id),
    branch_id               uuid        not null references branch (id),
    academic_year_id        uuid        not null references academic_year (id),
    grade_level             text        not null,
    name                    text        not null,
    capacity                integer     not null check (capacity > 0),
    class_teacher_person_id uuid references person (id),
    created_at              timestamptz not null,
    updated_at              timestamptz not null,
    version                 bigint      not null default 0,
    constraint uq_stream_name unique (branch_id, academic_year_id, name)
);
create index ix_stream_tenant on stream (tenant_id);

alter table role_assignment
    add constraint fk_role_assignment_stream foreign key (stream_id) references stream (id);

-- Default learning areas are seeded per tenant from the CBE catalogue and stay editable.
create table learning_area (
    id          uuid primary key,
    tenant_id   uuid        not null references tenant (id),
    code        text        not null,
    name        text        not null,
    level_band  text        not null,
    sort_order  integer     not null default 0,
    active      boolean     not null default true,
    created_at  timestamptz not null,
    updated_at  timestamptz not null,
    version     bigint      not null default 0,
    constraint uq_learning_area unique (tenant_id, level_band, code)
);
