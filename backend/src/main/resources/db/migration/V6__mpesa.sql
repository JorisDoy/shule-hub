create table mpesa_channel (
    id                    uuid primary key,
    tenant_id             uuid        not null references tenant (id),
    branch_id             uuid        not null references branch (id),
    shortcode             text        not null,
    party_b               text,
    channel_type          text        not null check (channel_type in ('PAYBILL', 'TILL')),
    environment           text        not null check (environment in ('SANDBOX', 'PRODUCTION')),
    consumer_key_enc      text        not null,
    consumer_secret_enc   text        not null,
    passkey_enc           text        not null,
    callback_token        text        not null unique,
    active                boolean     not null default true,
    created_at            timestamptz not null,
    updated_at            timestamptz not null,
    version               bigint      not null default 0,
    constraint uq_mpesa_channel unique (tenant_id, branch_id, shortcode)
);
create index ix_mpesa_channel_branch on mpesa_channel (tenant_id, branch_id, active);

create table payment_intent (
    id                      uuid primary key,
    tenant_id               uuid           not null references tenant (id),
    branch_id               uuid           not null references branch (id),
    channel_id              uuid           not null references mpesa_channel (id),
    learner_id              uuid           not null references learner (id),
    invoice_id              uuid references invoice (id),
    amount                  numeric(12, 2) not null check (amount >= 1),
    phone                   text           not null,
    account_reference       text           not null,
    idempotency_key         text           not null,
    merchant_request_id     text,
    checkout_request_id     text unique,
    status                  text           not null,
    result_code             text,
    result_desc             text,
    mpesa_receipt           text,
    requested_by_person_id  uuid references person (id),
    completed_at            timestamptz,
    created_at              timestamptz    not null,
    updated_at              timestamptz    not null,
    version                 bigint         not null default 0,
    constraint uq_payment_intent_idempotency unique (tenant_id, idempotency_key)
);
create index ix_payment_intent_learner on payment_intent (tenant_id, learner_id);

-- Raw Safaricom callbacks, kept for audit and replay. Written before the tenant is known.
create table mpesa_callback_log (
    id              uuid primary key,
    received_at     timestamptz not null,
    kind            text        not null,
    callback_token  text        not null,
    tenant_id       uuid,
    payload         text        not null,
    outcome         text
);
create index ix_mpesa_callback_log_received on mpesa_callback_log (received_at);
