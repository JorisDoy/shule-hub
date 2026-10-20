create table vote_head (
    id          uuid primary key,
    tenant_id   uuid        not null references tenant (id),
    code        text        not null,
    name        text        not null,
    priority    integer     not null default 100,
    created_at  timestamptz not null,
    updated_at  timestamptz not null,
    version     bigint      not null default 0,
    constraint uq_vote_head unique (tenant_id, code)
);

create table fee_structure (
    id           uuid primary key,
    tenant_id    uuid        not null references tenant (id),
    branch_id    uuid        not null references branch (id),
    term_id      uuid        not null references term (id),
    grade_level  text        not null,
    name         text        not null,
    status       text        not null default 'DRAFT',
    created_at   timestamptz not null,
    updated_at   timestamptz not null,
    version      bigint      not null default 0,
    constraint uq_fee_structure unique (branch_id, term_id, grade_level)
);
create index ix_fee_structure_tenant on fee_structure (tenant_id);

create table fee_structure_item (
    id                uuid primary key,
    tenant_id         uuid          not null references tenant (id),
    fee_structure_id  uuid          not null references fee_structure (id) on delete cascade,
    vote_head_id      uuid          not null references vote_head (id),
    amount            numeric(12, 2) not null check (amount > 0),
    applies_to        text          not null default 'ALL' check (applies_to in ('ALL', 'DAY', 'BOARDING')),
    created_at        timestamptz   not null,
    updated_at        timestamptz   not null,
    version           bigint        not null default 0
);
create index ix_fee_structure_item_structure on fee_structure_item (fee_structure_id);

create table invoice (
    id                uuid primary key,
    tenant_id         uuid           not null references tenant (id),
    branch_id         uuid           not null references branch (id),
    learner_id        uuid           not null references learner (id),
    term_id           uuid           not null references term (id),
    fee_structure_id  uuid references fee_structure (id),
    number            text           not null,
    description       text           not null,
    issued_on         date           not null,
    due_on            date           not null,
    status            text           not null default 'OPEN',
    total             numeric(12, 2) not null check (total >= 0),
    amount_paid       numeric(12, 2) not null default 0 check (amount_paid >= 0),
    created_at        timestamptz    not null,
    updated_at        timestamptz    not null,
    version           bigint         not null default 0,
    constraint uq_invoice_number unique (tenant_id, number),
    constraint uq_invoice_learner_term_structure unique (learner_id, term_id, fee_structure_id),
    constraint ck_invoice_paid check (amount_paid <= total)
);
create index ix_invoice_learner on invoice (tenant_id, learner_id);
create index ix_invoice_branch_status on invoice (tenant_id, branch_id, status);

create table invoice_line (
    id            uuid primary key,
    tenant_id     uuid           not null references tenant (id),
    invoice_id    uuid           not null references invoice (id) on delete cascade,
    vote_head_id  uuid           not null references vote_head (id),
    description   text           not null,
    priority      integer        not null,
    amount        numeric(12, 2) not null check (amount > 0),
    amount_paid   numeric(12, 2) not null default 0 check (amount_paid >= 0),
    created_at    timestamptz    not null,
    updated_at    timestamptz    not null,
    version       bigint         not null default 0,
    constraint ck_invoice_line_paid check (amount_paid <= amount)
);
create index ix_invoice_line_invoice on invoice_line (invoice_id);

create table payment (
    id                 uuid primary key,
    tenant_id          uuid           not null references tenant (id),
    branch_id          uuid           not null references branch (id),
    learner_id         uuid references learner (id),
    channel            text           not null,
    reference          text           not null,
    dedupe_key         text           not null,
    amount             numeric(12, 2) not null check (amount > 0),
    amount_allocated   numeric(12, 2) not null default 0 check (amount_allocated >= 0),
    payer_phone        text,
    payer_name         text,
    account_reference  text,
    received_at        timestamptz    not null,
    status             text           not null,
    raw_payload        text,
    created_at         timestamptz    not null,
    updated_at         timestamptz    not null,
    version            bigint         not null default 0,
    constraint uq_payment_dedupe unique (tenant_id, dedupe_key),
    constraint ck_payment_allocated check (amount_allocated <= amount)
);
create index ix_payment_learner on payment (tenant_id, learner_id);
create index ix_payment_branch_status on payment (tenant_id, branch_id, status);

create table payment_allocation (
    id               uuid primary key,
    tenant_id        uuid           not null references tenant (id),
    payment_id       uuid           not null references payment (id),
    invoice_line_id  uuid           not null references invoice_line (id),
    amount           numeric(12, 2) not null check (amount > 0),
    created_at       timestamptz    not null,
    updated_at       timestamptz    not null,
    version          bigint         not null default 0
);
create index ix_payment_allocation_payment on payment_allocation (payment_id);
create index ix_payment_allocation_line on payment_allocation (invoice_line_id);
