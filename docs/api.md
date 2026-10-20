# Shule Hub API contract (v0.1)

This is the contract between the Spring Boot backend and the Next.js frontend.
All JSON uses camelCase. Dates are ISO `YYYY-MM-DD`; instants are ISO-8601 UTC
(`2027-01-05T07:30:00Z`). Money is a JSON number in Kenya shillings with up to
two decimals (`12500.00`). IDs are UUID strings.

## Conventions

| Header | When | Meaning |
| --- | --- | --- |
| `Authorization: Bearer <jwt>` | every `/api/**` call except `/api/public/**` and `/api/callbacks/**` | Keycloak access token |
| `X-Tenant-Id: <uuid>` | every tenant-scoped call (all except `/api/me`, `/api/me/memberships`, `/api/platform/**`, `/api/public/**`, `/api/callbacks/**`) | Which school group the call acts on |

Errors are RFC 9457 problem details:

```json
{ "type": "about:blank", "title": "Bad Request", "status": 400,
  "detail": "Amount must be a whole number of shillings",
  "instance": "/api/fees/stk",
  "errors": [ { "field": "amount", "message": "must be greater than 0" } ] }
```

`errors` appears only for validation failures. Common statuses: 400 invalid input
or missing `X-Tenant-Id`, 401 no/expired token, 403 not allowed, 404 not found
(also returned for records in another tenant), 409 conflict / invalid state
transition, 502 M-Pesa gateway error.

Paged responses:

```json
{ "content": [ ... ], "page": 0, "size": 25, "totalElements": 132, "totalPages": 6 }
```

## Enumerations

- `GradeLevel`: `DAYCARE, PP1, PP2, GRADE_1, GRADE_2, GRADE_3, GRADE_4, GRADE_5, GRADE_6, GRADE_7, GRADE_8, GRADE_9`
- `LevelBand`: `DAYCARE, PRE_PRIMARY, LOWER_PRIMARY, UPPER_PRIMARY, JUNIOR_SCHOOL`
- `Role`: `GROUP_DIRECTOR, BRANCH_HEAD, DEPUTY, BURSAR, ADMISSIONS_OFFICER, CLASS_TEACHER, SUBJECT_TEACHER, PARENT`
- `ScopeType`: `GROUP, BRANCH, STREAM`
- `Gender`: `FEMALE, MALE`
- `BoardingStatus`: `DAY, BOARDING`
- `LearnerStatus`: `ACTIVE, WITHDRAWN, ALUMNI`
- `Relationship`: `MOTHER, FATHER, GUARDIAN, OTHER`
- `ApplicationStatus`: `ENQUIRY, APPLIED, ASSESSMENT_SCHEDULED, OFFERED, ACCEPTED, ADMITTED, WAITLISTED, REJECTED, WITHDRAWN`
- `FeeAppliesTo`: `ALL, DAY, BOARDING`
- `FeeStructureStatus`: `DRAFT, PUBLISHED`
- `InvoiceStatus`: `OPEN, PARTIALLY_PAID, PAID, VOID`
- `PaymentChannel`: `MPESA_STK, MPESA_C2B, BANK, CASH`
- `PaymentStatus`: `ALLOCATED, SUSPENSE, REVERSED` (ALLOCATED = matched to a learner, even if some of it is credit)
- `PaymentIntentStatus`: `PENDING, SUBMITTED, SUCCEEDED, FAILED`
- `AssessmentType`: `CAT, END_TERM, OBSERVATION, PROJECT`
- `AssessmentStatus`: `DRAFT, RELEASED`
- `ReportCardStatus`: `DRAFT, APPROVED, RELEASED`
- `Permission`: `TENANT_ADMIN, BRANCH_MANAGE, ACADEMICS_MANAGE, ADMISSIONS_VIEW, ADMISSIONS_MANAGE, LEARNERS_VIEW, LEARNERS_EDIT, FEES_VIEW, FEES_MANAGE, PAYMENTS_CONFIGURE, ASSESSMENT_ENTER, ASSESSMENT_MANAGE, REPORTS_RELEASE`

## Identity and tenancy

`GET /api/me` → `{ personId, givenName, familyName, email, phone, platformAdmin }`

`GET /api/me/memberships` →

```json
[ { "tenantId": "…", "tenantName": "Demo Academy", "tenantSlug": "demo",
    "grants": [ { "role": "BURSAR", "scopeType": "BRANCH", "branchId": "…", "branchName": "Ruaka", "streamId": null } ],
    "permissions": [ "FEES_VIEW", "FEES_MANAGE", "LEARNERS_VIEW", "PAYMENTS_CONFIGURE" ],
    "parent": false } ]
```

`parent` is true when the person is a guardian of at least one learner in that tenant.

`GET /api/branches` → `[ { id, name, code, county, boardingEnabled } ]`

`POST /api/branches` `{ name, code, county, boardingEnabled }` → branch (needs `BRANCH_MANAGE`)

`POST /api/platform/tenants` (platform admin) `{ name, slug, branch: { name, code, county, boardingEnabled }, director: { givenName, familyName, email, phone } }` → `{ tenantId, branchId, directorPersonId }`

## Academics

`GET /api/academic-years` → `[ { id, year, startDate, endDate, current, terms: [ { id, number, startDate, endDate } ] } ]` (newest first)

`POST /api/academic-years` `{ year, startDate, endDate, terms: [ { number, startDate, endDate } ] }` → academic year

`GET /api/terms/current` → `{ id, academicYearId, year, number, startDate, endDate }` (404 if today is outside every term: the most recent past term is returned instead when one exists)

`GET /api/streams?branchId=&academicYearId=` → `[ { id, branchId, academicYearId, gradeLevel, name, capacity } ]`

`POST /api/streams` `{ branchId, academicYearId, gradeLevel, name, capacity }` → stream

`GET /api/learning-areas?levelBand=` → `[ { id, code, name, levelBand, sortOrder } ]`

## Learners

`LearnerSummary`:

```json
{ "id": "…", "branchId": "…", "admissionNumber": "RUA0001", "givenName": "Amani",
  "familyName": "Otieno", "fullName": "Amani Otieno", "gradeLevel": "GRADE_4",
  "boardingStatus": "DAY", "status": "ACTIVE", "streamId": "…", "streamName": "Grade 4 East",
  "primaryContactPhone": "254712345678" }
```

`GET /api/learners?branchId=&gradeLevel=&streamId=&q=&status=&page=0&size=25` → page of `LearnerSummary` (`q` matches name or admission number)

`GET /api/learners/{id}` → `LearnerSummary` plus `{ otherNames, dateOfBirth, gender, upi, assessmentNumber, admittedOn, guardians: [ { personId, fullName, phone, email, relationship, feePayer, canPickUp, primaryContact } ] }`

`PATCH /api/learners/{id}` `{ upi?, assessmentNumber?, boardingStatus? }` → learner detail

`GET /api/streams/{id}/learners` → `[ LearnerSummary ]` (active enrolments, sorted by name)

## Admissions

Public enquiry (no token):

`POST /api/public/{tenantSlug}/enquiries`

```json
{ "branchCode": "RUA", "gradeLevel": "PP1", "boardingRequested": false,
  "learner": { "givenName": "Neema", "familyName": "Wanjiru", "dateOfBirth": "2023-03-14", "gender": "FEMALE" },
  "guardian": { "givenName": "Grace", "familyName": "Wanjiru", "phone": "0712345678", "email": "grace@example.com", "relationship": "MOTHER" },
  "notes": "Interested in the January intake" }
```

→ `201 { "reference": "ENQ-2026-0042" }`

`ApplicationSummary`: `{ id, reference, branchId, learnerName, gradeLevel, status, guardianName, guardianPhone, source, createdAt }`

`GET /api/admissions/applications?branchId=&status=` → `[ ApplicationSummary ]` newest first

`POST /api/admissions/applications` (staff) same body as the enquiry plus `{ branchId, academicYearId? }` (no `branchCode`) → `ApplicationDetail` with status `APPLIED`

`ApplicationDetail` = summary plus `{ academicYearId, learner: {…}, guardian: {…}, boardingRequested, notes, assessmentDate, offerExpiresOn, learnerId, admissionNumber, allowedTransitions: [ ApplicationStatus ], history: [ { fromStatus, toStatus, note, actorName, occurredAt } ] }`

`GET /api/admissions/applications/{id}` → `ApplicationDetail`

`POST /api/admissions/applications/{id}/transitions` `{ to, note?, assessmentDate?, offerExpiresOn? }` → `ApplicationDetail` (409 if not allowed; `ADMITTED` is not reachable here)

`POST /api/admissions/applications/{id}/admit` `{ streamId }` → `{ learnerId, admissionNumber }` (application must be `ACCEPTED`; stream grade must match; stream must have capacity)

Allowed transitions: `ENQUIRY → APPLIED | REJECTED | WITHDRAWN`; `APPLIED → ASSESSMENT_SCHEDULED | OFFERED | WAITLISTED | REJECTED | WITHDRAWN`; `ASSESSMENT_SCHEDULED → OFFERED | WAITLISTED | REJECTED | WITHDRAWN`; `WAITLISTED → OFFERED | REJECTED | WITHDRAWN`; `OFFERED → ACCEPTED | REJECTED | WITHDRAWN`; `ACCEPTED → WITHDRAWN` (and `ADMITTED` via `/admit`).

## Fees

`GET /api/fees/vote-heads` → `[ { id, code, name, priority } ]` (lower priority is paid first)

`POST /api/fees/vote-heads` `{ code, name, priority }`

`FeeStructure`: `{ id, branchId, termId, gradeLevel, name, status, dayTotal, boardingTotal, items: [ { id, voteHeadId, voteHeadName, amount, appliesTo } ] }`

`GET /api/fees/structures?branchId=&termId=` → `[ FeeStructure ]`

`POST /api/fees/structures` `{ branchId, termId, gradeLevel, name, items: [ { voteHeadId, amount, appliesTo } ] }` → `FeeStructure` (DRAFT; one per branch, term and grade)

`PUT /api/fees/structures/{id}` same body → `FeeStructure` (only while DRAFT)

`POST /api/fees/structures/{id}/publish` → `FeeStructure`

`POST /api/fees/invoices/generate` `{ branchId, termId, dueOn }` → `{ created, skipped }` (idempotent: one invoice per learner, term and structure)

`InvoiceSummary`: `{ id, number, learnerId, learnerName, admissionNumber, termId, issuedOn, dueOn, total, paid, balance, status }`

`GET /api/fees/invoices?branchId=&termId=&learnerId=&status=` → `[ InvoiceSummary ]`

`GET /api/fees/invoices/{id}` → summary plus `{ lines: [ { id, voteHeadName, description, amount, paid } ] }`

`GET /api/fees/learners/{learnerId}/statement` →

```json
{ "learnerId": "…", "learnerName": "Amani Otieno", "admissionNumber": "RUA0001",
  "totalInvoiced": 45000.00, "totalPaid": 30000.00, "balance": 15000.00,
  "entries": [ { "date": "2027-01-05", "type": "INVOICE", "reference": "INV-2027-000001",
                 "description": "Term 1 2027 fees", "debit": 45000.00, "credit": 0, "runningBalance": 45000.00 } ] }
```

`balance` > 0 means the family owes; < 0 is a credit.

`GET /api/fees/arrears?branchId=&asOf=` →

```json
{ "asOf": "2027-02-15",
  "totals": { "notDue": 0, "days1To30": 120000.00, "days31To60": 0, "days61To90": 0, "over90": 0, "total": 120000.00 },
  "learners": [ { "learnerId": "…", "learnerName": "…", "admissionNumber": "…", "gradeLevel": "GRADE_4",
                  "primaryContactPhone": "2547…", "notDue": 0, "days1To30": 15000.00, "days31To60": 0,
                  "days61To90": 0, "over90": 0, "total": 15000.00, "oldestDueOn": "2027-01-31" } ] }
```

`PaymentView`: `{ id, branchId, channel, reference, amount, allocated, unallocated, payerPhone, payerName, accountReference, learnerId, learnerName, receivedAt, status }`

`GET /api/fees/payments?branchId=&status=&learnerId=` → `[ PaymentView ]` newest first

`POST /api/fees/payments` `{ learnerId, amount, channel: "CASH" | "BANK", reference, receivedOn }` → `PaymentView`

`POST /api/fees/payments/{id}/assign` `{ learnerId }` → `PaymentView` (only for `SUSPENSE` payments)

`POST /api/fees/stk` `{ learnerId, amount, phone, invoiceId? }` → `StkInitiation`

`StkInitiation`: `{ intentId, status, customerMessage }` — amount must be a whole number ≥ 1

`GET /api/fees/stk/{intentId}` → `{ intentId, status, amount, phone, resultDesc, mpesaReceipt, updatedAt }`

## M-Pesa configuration and callbacks

`GET /api/payments/mpesa/channels` → `[ { id, branchId, shortcode, partyB, channelType, environment, active, callbackUrls: { stk, validation, confirmation } } ]`

`POST /api/payments/mpesa/channels` `{ branchId, shortcode, partyB?, channelType: "PAYBILL" | "TILL", environment: "SANDBOX" | "PRODUCTION", consumerKey, consumerSecret, passkey }` (needs `PAYMENTS_CONFIGURE`; secrets are encrypted at rest and never returned)

`POST /api/payments/mpesa/channels/{id}/register-c2b` → `{ responseDescription }`

Safaricom callbacks (no token; the random `callbackToken` in the path authenticates the channel):

- `POST /api/callbacks/mobile-money/{callbackToken}/stk`
- `POST /api/callbacks/mobile-money/{callbackToken}/c2b/validation`
- `POST /api/callbacks/mobile-money/{callbackToken}/c2b/confirmation`

## Assessment

`GradingScale`: `{ id, code, name, appliesTo: [ LevelBand ], bands: [ { code, label, minPercent, maxPercent, points, descriptor } ] }` (bands highest first)

`GET /api/assessment/grading-scales` → `[ GradingScale ]`

`Assessment`: `{ id, title, type, termId, streamId, streamName, gradeLevel, learningAreaId, learningAreaName, maxScore, weight, assessedOn, status, scoredCount, learnerCount }` — `maxScore` null means rubric-only (teacher picks a band per learner)

`GET /api/assessment/assessments?streamId=&termId=&learningAreaId=` → `[ Assessment ]`

`POST /api/assessment/assessments` `{ title, type, termId, streamId, learningAreaId, maxScore?, weight, assessedOn }` → `Assessment`

`GET /api/assessment/assessments/{id}/scores` →

```json
{ "assessment": { … }, "scale": { … GradingScale or null for daycare },
  "rows": [ { "learnerId": "…", "admissionNumber": "…", "learnerName": "…", "rawScore": 34, "rubricLevel": null,
              "percent": 85, "bandCode": "EE2", "comment": "Strong number sense" } ] }
```

`PUT /api/assessment/assessments/{id}/scores` `{ scores: [ { learnerId, rawScore?, rubricLevel?, comment? } ] }` → same as GET (409 once released)

`POST /api/assessment/assessments/{id}/release` → `Assessment`

`ReportCard`:

```json
{ "learner": { "id": "…", "fullName": "…", "admissionNumber": "…", "gradeLevel": "GRADE_7", "streamName": "Grade 7 Blue" },
  "term": { "id": "…", "year": 2027, "number": 1 },
  "scale": { "code": "KNEC_8_LEVEL", "name": "KNEC 8-level scale" },
  "areas": [ { "learningAreaId": "…", "name": "Mathematics", "percent": 78, "bandCode": "EE2",
               "bandLabel": "Exceeding Expectations 2", "points": 7, "assessmentCount": 3, "comment": "…" } ],
  "classTeacherComment": "…", "headComment": "…", "status": "DRAFT", "releasedAt": null }
```

`GET /api/assessment/report-cards?streamId=&termId=` → `[ { learnerId, learnerName, admissionNumber, status } ]`

`GET /api/assessment/report-cards/{learnerId}/{termId}` → `ReportCard`

`PUT /api/assessment/report-cards/{learnerId}/{termId}` `{ classTeacherComment?, headComment?, areaComments: [ { learningAreaId, comment } ] }` → `ReportCard` (409 once released)

`POST /api/assessment/report-cards/{learnerId}/{termId}/approve` → `ReportCard`

`POST /api/assessment/report-cards/release` `{ streamId, termId }` → `{ released }` (releases every APPROVED card in the stream)

## Parent portal

All need `X-Tenant-Id` and only return the caller's own children.

`GET /api/me/children` → `[ { learnerId, fullName, admissionNumber, gradeLevel, streamName, balance } ]`

`GET /api/me/children/{learnerId}/statement` → statement (as above)

`GET /api/me/children/{learnerId}/report-cards` → `[ { termId, year, number, releasedAt } ]`

`GET /api/me/children/{learnerId}/report-cards/{termId}` → `ReportCard` (released only)

`POST /api/me/children/{learnerId}/pay` `{ amount, phone }` → `StkInitiation`

`GET /api/me/children/{learnerId}/payments/{intentId}` → STK status (as `GET /api/fees/stk/{intentId}`)
