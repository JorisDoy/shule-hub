-- Tenancy and identity.
-- Every tenant-scoped table carries tenant_id; Hibernate's @TenantId filter adds
-- "tenant_id = ?" to every query and load, so rows from other tenants are invisible.

create table tenant (
    id          uuid primary key,
    name        text        not null,
    slug        text        not null unique check (slug ~ '^[a-z0-9][a-z0-9-]{1,62}$'),
    status      text        not null default 'ACTIVE',
    created_at  timestamptz not null,
    updated_at  timestamptz not null,
    version     bigint      not null default 0
);

create table branch (
    id               uuid primary key,
    tenant_id        uuid        not null references tenant (id),
    name             text        not null,
    code             text        not null,
    county           text,
    boarding_enabled boolean     not null default false,
    created_at       timestamptz not null,
    updated_at       timestamptz not null,
    version          bigint      not null default 0,
    constraint uq_branch_code unique (tenant_id, code)
);
create index ix_branch_tenant on branch (tenant_id);

-- One record per human, shared across tenants (a parent may have children in two schools).
create table person (
    id           uuid primary key,
    given_name   text        not null,
    family_name  text        not null,
    phone        text unique check (phone ~ '^254[17][0-9]{8}$'),
    email        text unique check (email = lower(email)),
    idp_subject  text unique,
    created_at   timestamptz not null,
    updated_at   timestamptz not null,
    version      bigint      not null default 0
);

create table role_assignment (
    id          uuid primary key,
    tenant_id   uuid        not null references tenant (id),
    person_id   uuid        not null references person (id),
    role        text        not null,
    scope_type  text        not null check (scope_type in ('GROUP', 'BRANCH', 'STREAM')),
    branch_id   uuid references branch (id),
    stream_id   uuid,
    valid_from  date,
    valid_to    date,
    created_at  timestamptz not null,
    updated_at  timestamptz not null,
    version     bigint      not null default 0,
    constraint ck_role_scope check (
        (scope_type = 'GROUP' and branch_id is null and stream_id is null)
        or (scope_type = 'BRANCH' and branch_id is not null and stream_id is null)
        or (scope_type = 'STREAM' and branch_id is not null and stream_id is not null))
);
create index ix_role_assignment_person on role_assignment (person_id);
create index ix_role_assignment_tenant on role_assignment (tenant_id);
create unique index ux_role_assignment on role_assignment (
    tenant_id, person_id, role, scope_type,
    coalesce(branch_id, '00000000-0000-0000-0000-000000000000'),
    coalesce(stream_id, '00000000-0000-0000-0000-000000000000'));

-- Human-readable numbers (admission numbers, invoice numbers) per tenant.
create table number_sequence (
    tenant_id   uuid   not null references tenant (id),
    name        text   not null,
    next_value  bigint not null,
    primary key (tenant_id, name)
);
