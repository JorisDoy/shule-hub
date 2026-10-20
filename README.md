# Shule Hub

School management for Kenyan CBE schools and school groups, Daycare to Grade 9: admissions,
learners and classes, fees with M-Pesa (STK Push, paybill and standing orders), CBE assessment
and report cards, and a parent portal.

This repository is the first working slice of the
[product blueprint](https://claude.ai/code/artifact/c9e2de9c-2a4a-4ca4-8991-e8a62e2d112c).

| Part | Stack | Folder |
| --- | --- | --- |
| API | Java 21, Spring Boot 4.1, Spring Modulith 2.1, Hibernate 7, Flyway, PostgreSQL 16 | `backend/` |
| Web app and public site | Next.js 16 (React 19), TypeScript, Tailwind CSS 4 | `frontend/` |
| Sign-in | Keycloak 26 (OpenID Connect, PKCE) | `infra/keycloak/` |
| API contract | Endpoint and JSON reference | `docs/api.md` |

## Run it locally

You need Docker, Java 21, Maven 3.9+ (`brew install maven`) and Node 22.

```bash
docker compose up -d                       # PostgreSQL on 5432, Keycloak on 8180

cd backend
SHULEHUB_DEMO_ENABLED=true mvn spring-boot:run   # API on http://localhost:8080 (seeds the demo school once)

cd ../frontend
cp .env.example .env.local
npm install
npm run dev                                # http://localhost:3000
```

Sign in at http://localhost:3000/app with any demo user below (password `Demo#2026!`):

| Login | Role in "Shule Hub Demo Academy" |
| --- | --- |
| director@demo.shulehub.local | Group director (everything) |
| bursar@demo.shulehub.local | Bursar, Ruaka campus |
| admissions@demo.shulehub.local | Admissions officer, Ruaka campus |
| deputy@demo.shulehub.local | Deputy (approves and releases report cards) |
| teacher@demo.shulehub.local | Class teacher, Grade 7 Blue |
| parent@demo.shulehub.local | Parent of Amani (Grade 7) and Neema (Grade 4) |
| admin@shulehub.local | Shule Hub platform admin (onboards new schools) |

API docs: http://localhost:8080/swagger-ui.html

### Testing M-Pesa against the Daraja sandbox

1. Create an app on the [Daraja portal](https://developer.safaricom.co.ke) and note its consumer key and secret.
2. Expose the API on a public HTTPS address (for example `ngrok http 8080`).
3. Start the backend with:
   ```bash
   SHULEHUB_DEMO_ENABLED=true \
   SHULEHUB_PUBLIC_BASE_URL=https://<your-tunnel> \
   SHULEHUB_MPESA_SANDBOX_CONSUMER_KEY=... \
   SHULEHUB_MPESA_SANDBOX_CONSUMER_SECRET=... \
   mvn spring-boot:run
   ```
   On a fresh database the demo seeder adds a sandbox paybill (shortcode 174379) to the Ruaka campus.
   Otherwise add one under Settings → M-Pesa in the app, then press "Register C2B URLs".
4. From a learner's statement, "Request M-Pesa payment" sends an STK prompt to the phone you give.

Daraja 3.0 keeps the same API paths; confirm them against the portal's current docs before going live.

## How the backend is organised

Each top-level package under `ke.shulehub` is a Spring Modulith module. A module's root package is
its public API; `internal` and `web` are private to it. `ModularityTests` fails the build if a module
reaches into another's internals or if modules depend on each other in a cycle.

| Module | Owns |
| --- | --- |
| `shared` | Tenant context, base entities, error handling, phone/money helpers |
| `identity` | People, roles and permissions, JWT security, the `X-Tenant-Id` filter |
| `tenancy` | School groups (tenants), branches, onboarding, memberships |
| `academics` | Academic years and terms, streams (classes), CBE learning areas |
| `learners` | Learner records, guardians, enrolments, admission numbers |
| `admissions` | Enquiry → application → offer → admission workflow |
| `payments` | M-Pesa channels, Daraja client, STK Push, Safaricom callbacks |
| `fees` | Vote heads, fee structures, invoices, the payment ledger, statements, arrears |
| `assessment` | Grading scales, assessments and marks, term report cards |
| `portal` | What parents see and do |
| `demo` | The demo school seeder |

Design points worth knowing:

- **Multi-tenancy.** Every school-owned row has `tenant_id`. Hibernate's `@TenantId` adds the tenant
  filter to every query and load, so another school's record simply isn't found (404). The tenant
  comes from the `X-Tenant-Id` header and is checked against the caller's roles. Native SQL must add
  `tenant_id` itself (only the sequence and lock helpers use any).
- **Money is recorded once.** M-Pesa receipts de-duplicate across STK and paybill; cash and bank
  entries de-duplicate per reference. Payments are split across a learner's unpaid invoices oldest
  first, by vote-head priority; anything left is credit, applied automatically to the next invoices.
- **No lost payments.** A Safaricom callback is logged raw, then confirmed in one transaction that
  also stores a `MpesaPaymentReceived` event (Spring Modulith's event publication table is the
  outbox). The fees ledger posts it asynchronously and is re-run on restart if it failed.
- **Grading is data.** The KNEC eight-level scale (Junior School) and the CBE four-level rubric
  (PP1 to Grade 6) are seeded per school and stored as bands, so a KNEC change is an edit, not a release.
  Percentages are rounded half-up to whole numbers before banding (74.5% → 75% → EE2).
- **Admission numbers double as M-Pesa account numbers**: branch code plus a sequence, letters and
  digits only (e.g. `RUA0042`), so parents can type them on any phone.

## Tests

```bash
cd backend && mvn verify      # unit tests + integration tests on PostgreSQL via Testcontainers (needs Docker)
cd frontend && npm test
```

GitHub Actions runs both on every push (`.github/workflows/ci.yml`).

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `SHULEHUB_DB_URL`, `SHULEHUB_DB_USER`, `SHULEHUB_DB_PASSWORD` | local Docker Postgres | Database |
| `SHULEHUB_ISSUER_URI` | `http://localhost:8180/realms/shulehub` | Keycloak realm that issues tokens |
| `SHULEHUB_CREDENTIALS_KEY` | a development key | Base64 32-byte AES key that encrypts M-Pesa credentials; **set your own** (`openssl rand -base64 32`) |
| `SHULEHUB_PUBLIC_BASE_URL` | `http://localhost:8080` | Public HTTPS address Safaricom calls back on |
| `SHULEHUB_CORS_ORIGINS` | `http://localhost:3000` | Browser origins allowed to call the API |
| `SHULEHUB_DEMO_ENABLED` | `false` | Seed the demo school on start-up |

## Not built yet

Planned in the blueprint but outside this slice: timetabling, LMS and online CATs, staff rotas and
anonymous service feedback, boarding operations, assets and fleet, multi-branch scorecards, SMS and
email delivery with consent records, KEMIS export, PDF report cards and the AI features (Spring AI
2.0's BOM is already pinned in `pom.xml`). Also still to do before production: PostgreSQL row-level
security as a second tenant guard, rate limiting on the public enquiry form, restricting Safaricom
callbacks to Safaricom's IP ranges, and a scheduled sweep that closes STK prompts whose callback
never arrived.
