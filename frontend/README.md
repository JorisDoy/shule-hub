# Shule Hub web frontend

Next.js frontend for Shule Hub, a school management platform for Kenyan private schools (Daycare, PP1–PP2, Grades 1–9, CBE curriculum). The API contract it is built against is `../docs/api.md`.

- `/`: marketing page, server-rendered (metadata, Open Graph, JSON-LD `SoftwareApplication`, `sitemap.xml`, `robots.txt` that disallows `/app`).
- `/app/**`: the school app. Client-side only, behind Keycloak sign-in.

## Run it

Requires Node 22.12 or newer.

```bash
cp .env.example .env.local   # adjust if your Keycloak or backend aren't on the defaults
npm install
npm run dev                  # http://localhost:3000
```

| Script | What it does |
| --- | --- |
| `npm run dev` | Dev server with hot reload |
| `npm run build` | Production build (includes type checking) |
| `npm start` | Serve the production build |
| `npm run lint` | ESLint (`eslint-config-next` core-web-vitals + TypeScript rules) |
| `npm run typecheck` | `tsc --noEmit` |
| `npm test` | Vitest unit and component tests (jsdom) |

## Environment variables

| Variable | Default | Used for |
| --- | --- | --- |
| `NEXT_PUBLIC_OIDC_AUTHORITY` | `http://localhost:8180/realms/shulehub` | Keycloak realm (OIDC issuer). Inlined into the browser bundle at build time. |
| `NEXT_PUBLIC_OIDC_CLIENT_ID` | `shulehub-web` | Public OIDC client ID. Inlined at build time. |
| `BACKEND_URL` | `http://localhost:8080` | Where `/api/:path*` is proxied (Next.js rewrite in `next.config.ts`). Server-side only; read when `next dev` / `next build` starts. |
| `NEXT_PUBLIC_SITE_URL` | `https://shulehub.example` | Optional. Canonical URL for metadata, sitemap and robots. |

Keycloak client settings: public client, Standard Flow on, PKCE `S256`, valid redirect URI `<origin>/app`, post-logout redirect `<origin>/`, web origin `<origin>`.

## How sign-in works

`src/features/auth/AuthRoot.tsx` wraps `/app` in `react-oidc-context` (`oidc-client-ts` underneath), using authorization code + PKCE with a public client.

1. Opening any `/app` page without a session remembers the deep link (sessionStorage) and redirects to Keycloak.
2. Keycloak returns to `redirect_uri = window.location.origin + "/app"`. The library exchanges the code, then `onSigninCallback` removes `?code=&state=` from the address bar with `history.replaceState`.
3. Tokens renew silently (`automaticSilentRenew`). A 401 from the API sends the person back through sign-in once and returns them to the same page.
4. Sign out uses Keycloak's end-session endpoint and comes back to `/`.

## How tenant (school) selection works

`src/features/session/SessionProvider.tsx`:

1. After sign-in it calls `GET /api/me/memberships` (no `X-Tenant-Id`).
2. No memberships: a "your login isn't linked to a school yet" screen.
3. One membership: used directly. Several: a school picker. The choice is stored in `localStorage` under `shulehub.tenantId` (all storage access is wrapped in try/catch, so private browsing only loses the preference). "Switch school" in the user menu clears it.
4. From then on every tenant-scoped call sends `Authorization: Bearer <token>` and `X-Tenant-Id: <tenantId>`. `/api/me`, `/api/me/memberships`, `/api/platform/**`, `/api/public/**` and `/api/callbacks/**` don't get the tenant header; `/api/me/children/**` does (`src/lib/api/client.ts`).
5. Landing page: memberships with any staff permission go to `/app`. Parent-only memberships (`permissions` empty, `parent: true`) go to `/app/parent`. Staff who are also parents get a "Parent view" / "Staff view" switch. Visiting a page the membership can't use redirects to its home (`src/lib/permissions.ts`).
6. Staff pick a branch in the top bar (`GET /api/branches`); it's remembered per school in `localStorage`. Lists such as admissions, learners, fees and streams are scoped to that branch.

Navigation is filtered by permission: Admissions `ADMISSIONS_VIEW`, Learners `LEARNERS_VIEW`, Fees `FEES_VIEW`, Assessment `ASSESSMENT_ENTER` or `ASSESSMENT_MANAGE`, Settings `ACADEMICS_MANAGE` or `PAYMENTS_CONFIGURE`. Actions inside a page also check the matching `*_MANAGE`, `LEARNERS_EDIT` or `REPORTS_RELEASE` permission.

## Code layout

```
src/
  app/                    routes (marketing at /, app under /app)
  components/ui/          hand-rolled Button, Input/Select/Textarea, Table, Badge, Card, Dialog, Tabs, Alert/Toast, EmptyState
  features/               feature components (auth, session, shell, admissions, fees, assessment, report-cards, payments, parent, marketing)
  lib/api/types.ts        types mirroring docs/api.md
  lib/api/client.ts       fetch wrapper: auth + tenant headers, RFC 9457 problem parsing
  lib/api/endpoints.ts    typed function per endpoint
  lib/api/problem.ts      ApiError, problem-detail parsing, field-error lookup
  lib/hooks.ts            useQuery / useAction (tiny data-fetching hooks with loading, error and refetch)
  lib/format.ts           KES (Intl en-KE), dates like "5 Jan 2027", EAT times
  lib/phone.ts            07…/01…/+254…/254… → 2547XXXXXXXX / 2541XXXXXXXX
  lib/grades.ts           grade labels and grade → CBE level band
  lib/bands.ts            band preview (percent rounded half-up, highest minPercent ≤ percent)
  lib/permissions.ts      nav items and route access from permissions
```

Band preview matches the backend: `percent = rawScore / maxScore × 100`, rounded half-up to a whole number using integer arithmetic (so 149/200 → 74.5 → 75), then the band with the highest `minPercent` ≤ percent (75 → EE2 on the KNEC 8-level scale).

M-Pesa payments (staff on a learner's page, parents on the child card) call `POST /api/fees/stk` or `POST /api/me/children/{id}/pay`, then poll the intent every 3 seconds for up to 2 minutes and show pending, success with receipt, failure with reason, or a "still waiting" message.

## Design notes

- Fonts are self-hosted from `src/app/fonts` (Besley for headings, Atkinson Hyperlegible Next for everything else; both SIL OFL, Latin subset, about 70 KB together). No requests to Google Fonts.
- Documents (report cards, statements, the admission number) are drawn as exercise-book paper with the red margin line; the violet "stamp" is reserved for approved and released states.
- Report cards and statements print on A4: print CSS shows only `.print-area`.
- No UI framework or chart library. Tables collapse to stacked rows on phones and every page was checked at 360 px wide without horizontal scrolling.

## Versions and deviations

Pinned exactly in `package.json`: Next.js 16.4.2, React 19.3.1, Tailwind CSS 4.3.3, lucide-react 1.60.0, oidc-client-ts 3.5.0, react-oidc-context 3.3.1, Vitest 5.0.5, Testing Library React 16.3.3, jsdom 29.1.1.

- **TypeScript 6.0.3, not 7.0.2.** With TypeScript 7.0.2 (the native Go port), `tsc --noEmit` and `next build` type checking both worked, but `npm run lint` crashed: `eslint-config-next@16.4.2` depends on `typescript-eslint@8.72.0`, which declares `typescript >=4.8.4 <6.1.0` and throws "typescript-eslint does not support TS 7.0" at load. 6.0.3 is the newest version that satisfies it. Revisit when typescript-eslint supports TS 7.
- **ESLint 9.39.5, not 10.** ESLint 10 runs, but `eslint-plugin-react`, `eslint-plugin-import` and `eslint-plugin-jsx-a11y` (bundled by `eslint-config-next`) declare peer support only up to ESLint 9, and `create-next-app@16.4.2` itself scaffolds `eslint@^9`. npm marks 9.39.5 as deprecated; move to 10 once those plugins add support.
- **jsdom 29.1.1, not 30.x.** jsdom 30 requires Node ≥ 22.22.2; 29.1.1 runs on Node 22.12+.
