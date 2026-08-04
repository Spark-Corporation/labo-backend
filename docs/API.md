# LIMS Backend — REST API Reference

Audience: the LIMS frontend team. This document describes **every** HTTP endpoint the
backend exposes — 188 application endpoints across 11 modules, plus the 3 authentication
endpoints contributed by the `secured-x` starter.

> **Provenance and status.** Hand-derived from the controller sources at commit `9244f34`
> (2026-08-04), then superseded the same day: `springdoc-openapi` is now wired into the
> build, so a **generated** schema is available and is authoritative wherever the two
> disagree.
>
> - Swagger UI (`dev` profile): <http://localhost:8080/swagger-ui.html>
> - Schema: <http://localhost:8080/v3/api-docs>, or `target/openapi.json` after `./mvnw verify`
>
> This file is kept as a readable offline reference — it carries narrative the generated
> schema does not (integration order, the discount-fraction trap, the two pagination
> shapes, the permission→role matrix). It is **not** regenerated automatically and *will*
> drift as endpoints change. Retire it once the frontend team confirms the Swagger console
> covers their needs.

---

## 1. Base URL and versioning

| | |
|---|---|
| Local dev | `http://localhost:8080` |
| Application prefix | `/api/v1` |
| Auth prefix | `/auth` (**not** under `/api/v1`) |
| Ops | `/actuator/health`, `/actuator/info`, `/actuator/prometheus` |

Everything is JSON (`application/json`) unless the endpoint is explicitly a file download
or a multipart upload. `v1` is the only version; there is no content negotiation on
version.

---

## 2. Authentication

### 2.1 Staff realm (the main API)

Three endpoints, served by the `secured-x` starter, all public:

#### `POST /auth/login`

```jsonc
// request
{
  "provider": "local",
  "payload": { "username": "admin@lab.local", "password": "…" }
}
```

```jsonc
// 200 response
{
  "accessToken":  "eyJhbGciOiJSUzI1NiIs…",
  "refreshToken": "eyJhbGciOiJSUzI1NiIs…",
  "expiresIn":    86400,          // seconds
  "subject":      "01KQZ0WPVQF61G08X2040B5EYA"   // the user's ULID
}
```

`provider` is `"local"` for username/password. `payload` is a free-form map whose keys are
provider-specific; the local provider reads `username` and `password`. Bad credentials come
back as `401` with no body distinction between "unknown user" and "wrong password".

#### `POST /auth/refresh`

```jsonc
{ "refreshToken": "…" }   // → same LoginResponse shape as /auth/login
```

#### `POST /auth/logout`

No body, no response body. Requires the bearer token.

### 2.2 Using the token

Attach the access token to every `/api/v1/**` request:

```http
Authorization: Bearer <accessToken>
```

Token lifetimes come from config: access **24 h** (`86400000` ms), refresh **7 days**
(`604800000` ms). Tokens are RS256-signed; the frontend must not attempt to validate or
introspect them, only carry them.

A missing or expired token yields `401`. A *valid* token that lacks the required permission
yields `403` — see §3.

### 2.3 Patient portal realm (separate, do not mix)

The patient portal is a **second, independent auth realm** gated by its own servlet filter
on `/api/v1/portal/*`. Portal tokens are **not** staff JWTs and staff JWTs do **not** work
on portal endpoints.

#### `POST /api/v1/portal/login`

```jsonc
// request
{ "portalLogin": "…", "password": "…" }

// 200 response
{ "token": "…", "expiresInSeconds": 1800 }   // 30-minute RS256 portal token
```

Returns `401` with an empty body on failure — deliberately indistinguishable between an
unknown login and a bad password, to prevent account enumeration.

Carry the portal token the same way (`Authorization: Bearer <token>`) but only against
`/api/v1/portal/**`. Only two portal endpoints exist: the dossier list and the dossier PDF.

---

## 3. Authorization

Every application endpoint (except `/api/v1/me*` and the portal) is gated by a single named
permission, declared server-side with `@RequiresPermission("…")`. There are **51
permissions** mapped onto **7 roles**. A user holds roles; roles carry permissions.

Two consequences for the frontend:

1. **Drive the UI off permissions, not roles.** Roles are seed data and will be re-cut;
   permission names are the stable contract. Each endpoint below names its permission, and
   Appendix A gives the full permission → role matrix.
2. **`403` is a normal, expected response**, not an error to log loudly. It means the token
   is fine but this user cannot perform this action. Prefer hiding or disabling the control
   rather than letting the user discover it via a `403`. Note the `403` body is *not* the
   RFC 7807 envelope the rest of the API uses — see §5.1.

`GET /api/v1/me` returns the current user including their `roles`, but the *permission* set
is not currently exposed on that response — if you need permission-driven UI gating and
don't want to hard-code the role→permission table, say so and we will add it to
`UserResponse` (small, additive change).

Roles: `ADMIN`, `LAB_MANAGER`, `BIOLOGIST`, `TECHNICIAN`, `RECEPTIONIST`, `CASHIER`,
`ACCOUNTANT`.

> Note one deliberate quirk of the seed data: `ADMIN` does **not** hold the `cashbox.*`
> permissions. Cash handling is segregated to `LAB_MANAGER` / `CASHIER` / `RECEPTIONIST` /
> `ACCOUNTANT` by design. Don't assume "admin can do everything".

---

## 4. Conventions

### 4.1 Identifiers are ULIDs

Every resource id is a **26-character Crockford-base32 ULID**, e.g.
`01KQZ0WPVQF61G08X2040B5EYA`. They are lexicographically sortable by creation time. Treat
them as opaque strings — never parse, never assume numeric. Several endpoints validate the
shape with `^[0-9A-HJKMNP-TV-Z]{26}$`: a malformed id **in a request body** is a `422`; in a
query parameter it is a `400` on a `@Validated` controller and, per the caveat in §4.3, a
`500` on the others.

Human-facing codes are separate from ids and are **not** interchangeable with them:
`registrationNumber` on a lab file, `number` on an invoice, `labCode` on a patient.

### 4.2 Action endpoints use a `:verb` suffix

State transitions are not modelled as `PATCH`. They are `POST` to a colon-suffixed path:

```
POST /api/v1/lab-files/{id}:publish
POST /api/v1/lab-files/{id}:sign
POST /api/v1/results/{id}:validate-technical
POST /api/v1/quotes/{id}:lock
POST /api/v1/cashbox/sessions/{cashRegisterId}/{sessionDate}:close
```

The colon is a literal character in the path — **do not URL-encode it** as `%3A`. Most HTTP
clients leave it alone; verify yours does.

### 4.3 Pagination — two shapes, know which you're calling

**Shape A (most endpoints, 22 of them).** Explicit `page` / `size` query params, and an
envelope in the response:

```jsonc
{ "content": [ … ], "page": 0, "size": 20, "totalElements": 137, "totalPages": 7 }
```

`page` is 0-based, `size` defaults to `20`. A `size` cap is declared in source — `100` on
most endpoints, `200` on the worklist/queue endpoints (`/dossiers`, `/results/search`, the
`queues/*` family, `/samples/queues/destruction-due`).

> ⚠ **An over-cap `size` returns `500` on most endpoints.** Verified against a running
> server on 2026-08-04. Only 7 of the 19 controllers that declare `@Min`/`@Max` on their
> paging params also carry `@Validated`; on those you get a clean `400`:
>
> ```
> GET /api/v1/dossiers?size=99999   → 400  "list.size: must be less than or equal to 200"
> ```
>
> On the other 12 — including `/api/v1/patients` and `/api/v1/admin/users` — the bound
> still fires but the exception is unmapped and falls through to the generic handler:
>
> ```
> GET /api/v1/patients?size=99999   → 500  {"detail":"internal error", …}
> ```
>
> **Clamp `size` client-side.** Never send an unbounded page size expecting a validation
> error back. This is a backend bug, tracked as a follow-up; when it is fixed all endpoints
> will return `400` and nothing you built against the clamp will break.

**Shape B (6 endpoints).** Spring `Pageable` binding — `GET /api/v1/cash-registers`,
`/api/v1/insurances`, `/api/v1/partners`, `/api/v1/cashbox/expenses`,
`/api/v1/cashbox/home-collection-fees`, `/api/v1/cashbox/journal`. These accept
`page`, `size` **and** `sort=field,asc|desc`, and return Spring's full `Page` JSON, which
carries `pageable`, `sort`, `first`, `last`, `numberOfElements` alongside `content`. Read
`content` and `totalElements` and ignore the rest.

Endpoints not listed in either group return a plain array or a bare object — no pagination.

### 4.4 Dates, times, numbers

| Java type | JSON | Example |
|---|---|---|
| `Instant` | ISO-8601 UTC, `Z` suffix | `"2026-08-04T09:31:00Z"` |
| `LocalDate` | ISO date, no zone | `"2026-08-04"` |
| `BigDecimal` | JSON **number** | `12500.00` |
| enum | its **name** string | `"BIOLOGICALLY_VALIDATED"` |

Enums are never ordinals. Sending an unknown enum name is a `400`.

**Money is XOF (CFA franc).** There is no currency field anywhere because the system is
single-currency. Amounts are `BigDecimal` and serialise as JSON numbers — if your client
parses JSON numbers as IEEE-754 doubles, prefer a decimal-safe parser for invoice
arithmetic.

**Discounts:** `discountKind: "AMOUNT"` means `discountValue` is an absolute XOF amount;
`discountKind: "PERCENT"` means `discountValue` is a **fraction in `[0, 1]`** — `0.15`, not
`15`. This is a common source of 100×-off bugs.

### 4.5 Concurrency and the `version` field

Many responses expose a `version` integer (JPA optimistic-lock counter). Today it is
**informational only** — no endpoint accepts a client-supplied version and there is no
`If-Match` support. If two writers genuinely race, the loser gets:

```
409 Conflict — type https://lims/errors/conflict, detail "concurrent modification — retry"
```

Handle `409` on every mutation by re-fetching and re-presenting, not by blind retry.

### 4.6 `Idempotency-Key` is accepted but not yet honoured

`POST /api/v1/patients` accepts an optional `Idempotency-Key` header. **It is currently
read and discarded — there is no dedupe behaviour behind it.** Sending it is harmless and
future-proof, but do not rely on it to make patient creation safe to retry. Treat every
`POST` as non-idempotent for now.

### 4.7 Response signing headers

Mutating requests come back carrying an audit-chain signature:

```http
X-Action-Id:        01KRXY4CP5XBW747K8245Z8VRQ
X-Action-KeyId:     …
X-Action-Signature: …
```

This is **server-side** signing for the tamper-evident audit trail. The frontend does
**not** sign requests and does not need to verify these — just don't strip them in a proxy.

### 4.8 File downloads

16 endpoints return binary rather than JSON. They set
`Content-Disposition: attachment; filename="…"` and a real `Content-Type` (mostly
`application/pdf`, plus `.docx`/`.xlsx` for the Office exports). Request them with a
`Blob`/`ArrayBuffer` response type — a JSON parser will choke. The filename in
`Content-Disposition` is the one to show the user.

Note `GET /api/v1/pdf-jobs/{id}` is the async-job counterpart: poll it and it returns the
PDF once the job status reaches `DONE` (`PdfRenderJobStatus`: `QUEUED`, `RUNNING`, `DONE`,
`FAILED`).

### 4.9 File uploads

Three attachment endpoints take `multipart/form-data` with a `file` part. The cap is
**25 MiB per file / 26 MiB per request**; exceeding it is a `413`. Allowed targets are lab
files, cashbox expenses, and home-collection fees.

---

## 5. Error model

**Most** errors are [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807)
`application/problem+json` — the two exceptions, `401` and `403`, are covered in §5.1. A
real `422` response, copied verbatim from a running server:

```jsonc
{
  "type":     "https://lims/errors/validation",
  "title":    "Validation failed",
  "status":   422,
  "detail":   "2 field(s) invalid",
  "instance": "/api/v1/patients",
  "traceId":  "unknown",                        // always "unknown" today — see §5.2
  "errors": [                                   // validation only
    { "field": "contact",  "code": "NotNull", "message": "must not be null" },
    { "field": "civility", "code": "NotNull", "message": "must not be null" }
  ]
}
```

| Status | `type` | When |
|---|---|---|
| `400` | `…/errors/bad-request` | Malformed input: bad enum name, missing required query param, wrong parameter type, constraint violation |
| `404` | `…/errors/not-found` | Unknown id. `detail` carries a human-readable message |
| `409` | `…/errors/conflict` | Optimistic-lock race, or a domain-state conflict (e.g. printing a CR whose results aren't biologically validated) |
| `413` | `/problems/attachment/too-large` | Upload over the 25 MiB cap |
| `422` | `…/errors/validation` | Request body failed bean validation. **This is where field-level errors live**, not `400` |
| `500` | `…/errors/internal` | `detail` is always the literal `"internal error"`; stack traces are logged server-side, never returned |

### 5.1 `401` and `403` do NOT use this envelope

Both are produced by the `secured-x` security layer before the application's exception
advice runs, so they look nothing like the table above. Verified 2026-08-04:

**`401`** — status line only. **Empty body**, `Content-Length: 0`. Do not try to parse it.

**`403`** — a different JSON shape, with no `type`, no `instance`, and no `traceId`:

```jsonc
{
  "error":   "authorization_denied",
  "message": "Required permission(s): catalog.manage (match=ANY)",
  "status":  403
}
```

Note it *does* name the missing permission, which is useful for debugging but means the
message is not safe to show a user verbatim. Branch on the HTTP status, and read `message`
only for developer-facing diagnostics.

### 5.2 `traceId` is currently always `"unknown"`

Every RFC 7807 body carries a `traceId`, but the MDC field that feeds it is not populated
on request threads today — every response comes back with the literal string `"unknown"`,
and server log lines show an empty `traceId=`. Verified 2026-08-04.

Read the field and display it if present, but **do not build a support workflow around it
yet** and do not tell users it identifies their request. This is a backend gap; once fixed,
the field becomes meaningful with no client change.

### 5.3 Branch on `status`, not on `type`

Module-specific advices add finer `type` URIs under the same envelope. Be aware the `type`
namespace is **not consistent**: the shared advice emits absolute URIs
(`https://lims/errors/not-found`) while the attachment/module advices emit relative paths
(`/problems/attachment/too-large`). **Branch on `status` first** and treat `type` as a
refinement — do not build routing logic on the `type` prefix alone.

Also note `422` arrives from two different places with two different bodies: bean-validation
failures carry the `errors[]` array shown above; domain rejections (e.g. an empty attachment)
carry only `detail`. Guard your parser — `errors` may be absent on a `422`.

---

## 6. Suggested integration order

The domain is a pipeline; building the frontend in workflow order avoids blocking:

1. **Auth + `/api/v1/me`** — log in, hold a token, render the shell.
2. **Catalog reads** — analyses, disciplines, recipients, prescribers, partners, insurances.
   These populate every downstream picker.
3. **Patients** — create, search, update.
4. **Lab files** — create a dossier, attach analyses, record samples.
5. **Results** — enter, then technical then biological validation.
6. **Delivery** — sign, publish, send, print.
7. **Billing + cashbox** — invoices, payments, expenses, day-close.
8. **Stats** — dashboards last; they read everything above.

---

## 7. Endpoints

### Identity, users & audit

| Method | Path | Permission | Success |
|---|---|---|---|
| `GET` | `/api/v1/admin/users` | `users.manage` | 200 OK |
| `POST` | `/api/v1/admin/users` | `users.manage` | 201 Created |
| `DELETE` | `/api/v1/admin/users/{id}` | `users.manage` | 204 No Content |
| `GET` | `/api/v1/admin/users/{id}` | `users.manage` | 200 OK |
| `PATCH` | `/api/v1/admin/users/{id}` | `users.manage` | 200 OK |
| `PUT` | `/api/v1/admin/users/{id}/roles` | `users.manage` | 200 OK |
| `GET` | `/api/v1/audit/actions` | `audit.read` | 200 OK |
| `POST` | `/api/v1/audit/checkpoints` | `audit.checkpoint` | 201 Created |
| `GET` | `/api/v1/audit/checkpoints/latest` | `audit.read` | 200 OK |
| `GET` | `/api/v1/audit/lab-files/{labFileId}/actions` | `audit.read` | 200 OK |
| `POST` | `/api/v1/audit/verify` | `audit.read` | 200 OK |
| `GET` | `/api/v1/me` | — | 200 OK |
| `PATCH` | `/api/v1/me/password` | — | 204 No Content |

<details>
<summary><code>GET /api/v1/admin/users</code></summary>

- **Permission:** `users.manage`
- **Success:** 200 OK
- **Query params:**
  - `q` — `String`, optional
  - `role` — `String`, optional
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `UserSearchResponse`
  - `content` — `List<UserResponse>`
    - `id` — `String`
    - `email` — `String`
    - `fullName` — `String`
    - `enabled` — `boolean`
    - `roles` — `Set<Role>` — one of: ADMIN, LAB_MANAGER, BIOLOGIST, TECHNICIAN, RECEPTIONIST, CASHIER, ACCOUNTANT
    - `lastLoginAt` — `Instant`
    - `createdAt` — `Instant`
    - `updatedAt` — `Instant`
    - `version` — `int`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>POST /api/v1/admin/users</code></summary>

- **Permission:** `users.manage`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `CreateUserRequest`
  - `email` — `String`
  - `fullName` — `String`
  - `password` — `String`
  - `roles` — `Set<Role>` — one of: ADMIN, LAB_MANAGER, BIOLOGIST, TECHNICIAN, RECEPTIONIST, CASHIER, ACCOUNTANT
- **Response body:** `UserResponse`
  - `id` — `String`
  - `email` — `String`
  - `fullName` — `String`
  - `enabled` — `boolean`
  - `roles` — `Set<Role>` — one of: ADMIN, LAB_MANAGER, BIOLOGIST, TECHNICIAN, RECEPTIONIST, CASHIER, ACCOUNTANT
  - `lastLoginAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>DELETE /api/v1/admin/users/{id}</code></summary>

- **Permission:** `users.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/admin/users/{id}</code></summary>

- **Permission:** `users.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `UserResponse`
  - `id` — `String`
  - `email` — `String`
  - `fullName` — `String`
  - `enabled` — `boolean`
  - `roles` — `Set<Role>` — one of: ADMIN, LAB_MANAGER, BIOLOGIST, TECHNICIAN, RECEPTIONIST, CASHIER, ACCOUNTANT
  - `lastLoginAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>PATCH /api/v1/admin/users/{id}</code></summary>

- **Permission:** `users.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `UpdateUserRequest`
  - `email` — `String`
  - `fullName` — `String`
  - `enabled` — `Boolean`
- **Response body:** `UserResponse`
  - `id` — `String`
  - `email` — `String`
  - `fullName` — `String`
  - `enabled` — `boolean`
  - `roles` — `Set<Role>` — one of: ADMIN, LAB_MANAGER, BIOLOGIST, TECHNICIAN, RECEPTIONIST, CASHIER, ACCOUNTANT
  - `lastLoginAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>PUT /api/v1/admin/users/{id}/roles</code></summary>

- **Permission:** `users.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `AssignRolesRequest`
  - `roles` — `Set<Role>` — one of: ADMIN, LAB_MANAGER, BIOLOGIST, TECHNICIAN, RECEPTIONIST, CASHIER, ACCOUNTANT
- **Response body:** `UserResponse`
  - `id` — `String`
  - `email` — `String`
  - `fullName` — `String`
  - `enabled` — `boolean`
  - `roles` — `Set<Role>` — one of: ADMIN, LAB_MANAGER, BIOLOGIST, TECHNICIAN, RECEPTIONIST, CASHIER, ACCOUNTANT
  - `lastLoginAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>GET /api/v1/audit/actions</code></summary>

- **Permission:** `audit.read`
- **Success:** 200 OK
- **Query params:**
  - `subject` — `String`, optional
  - `resource` — `String`, optional
  - `action` — `String`, optional
  - `from` — `String`, optional
  - `to` — `String`, optional
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `AuditActionSearchResponse`
  - `content` — `List<AuditActionResponse>`
    - `actionId` — `String`
    - `subject` — `String`
    - `action` — `String`
    - `resource` — `String`
    - `timestamp` — `Instant`
    - `correlationId` — `String`
    - `sequenceNumber` — `long`
    - `metadata` — `Map<String, Object>`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>POST /api/v1/audit/checkpoints</code></summary>

- **Permission:** `audit.checkpoint`
- **Success:** 201 Created
- **Response body:** `AuditCheckpointResponse`
  - `checkpointId` — `String`
  - `globalSequence` — `long`
  - `timestamp` — `Instant`
  - `merkleRoot` — `String`
  - `subjectCount` — `int`

</details>

<details>
<summary><code>GET /api/v1/audit/checkpoints/latest</code></summary>

- **Permission:** `audit.read`
- **Success:** 200 OK
- **Response body:** `CheckpointVerifyResponse`
  - `checkpointId` — `String`
  - `globalSequence` — `long`
  - `timestamp` — `Instant`
  - `merkleRoot` — `String`
  - `subjectCount` — `int`
  - `signatureValid` — `boolean`
  - `headsMatch` — `boolean`
  - `actionsAppendedSince` — `long`

</details>

<details>
<summary><code>GET /api/v1/audit/lab-files/{labFileId}/actions</code></summary>

- **Permission:** `audit.read`
- **Success:** 200 OK
- **Path params:** `labFileId` (`String`)
- **Query params:**
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `AuditActionSearchResponse`
  - `content` — `List<AuditActionResponse>`
    - `actionId` — `String`
    - `subject` — `String`
    - `action` — `String`
    - `resource` — `String`
    - `timestamp` — `Instant`
    - `correlationId` — `String`
    - `sequenceNumber` — `long`
    - `metadata` — `Map<String, Object>`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>POST /api/v1/audit/verify</code></summary>

- **Permission:** `audit.read`
- **Success:** 200 OK
- **Query params:**
  - `subject` — `String`, required
- **Response body:** `Map<String, Object>`

</details>

<details>
<summary><code>GET /api/v1/me</code></summary>

- **Permission:** authenticated only
- **Success:** 200 OK
- **Response body:** `UserResponse`
  - `id` — `String`
  - `email` — `String`
  - `fullName` — `String`
  - `enabled` — `boolean`
  - `roles` — `Set<Role>` — one of: ADMIN, LAB_MANAGER, BIOLOGIST, TECHNICIAN, RECEPTIONIST, CASHIER, ACCOUNTANT
  - `lastLoginAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>PATCH /api/v1/me/password</code></summary>

- **Permission:** authenticated only
- **Success:** 204 No Content
- **Request body:** `ChangePasswordRequest`
  - `oldPassword` — `String`
  - `newPassword` — `String`

</details>


### Patients

| Method | Path | Permission | Success |
|---|---|---|---|
| `GET` | `/api/v1/patients` | `patient.search` | 200 OK |
| `POST` | `/api/v1/patients` | `patient.create` | 201 Created |
| `DELETE` | `/api/v1/patients/{id}` | `patient.update` | 204 No Content |
| `GET` | `/api/v1/patients/{id}` | `patient.search` | 200 OK |
| `PATCH` | `/api/v1/patients/{id}` | `patient.update` | 200 OK |
| `POST` | `/api/v1/patients/{id}/merge` | `patient.merge` | 200 OK |

<details>
<summary><code>GET /api/v1/patients</code></summary>

- **Permission:** `patient.search`
- **Success:** 200 OK
- **Query params:**
  - `name` — `String`, optional
  - `phone` — `String`, optional
  - `idDocumentNumber` — `String`, optional
  - `labCode` — `String`, optional
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `PatientSearchResponse`
  - `content` — `List<PatientResponse>`
    - `id` — `String`
    - `labCode` — `String`
    - `name` — `Name`
      - `lastName` — `String`
      - `firstNames` — `String`
      - `maidenName` — `String`
    - `civility` — `Civility` — one of: MONSIEUR, MADAME, MADEMOISELLE, ENFANT, NOUVEAU_NE
    - `sex` — `Sex` — one of: M, F
    - `birthDate` — `LocalDate`
    - `idDocument` — `IdDoc`
      - `type` — `IdDocumentType` — one of: CARTE_IDENTITE, PASSEPORT, PERMIS_CONDUIRE, AUTRE
      - `number` — `String`
    - `profession` — `String`
    - `address` — `String`
    - `clinicalNotes` — `String`
    - `contact` — `Contact`
      - `phone` — `String`
      - `email` — `String`
    - `guardian` — `Guardian`
      - `name` — `String`
      - `phone` — `String`
      - `relationship` — `GuardianRelationship` — one of: PARENT, TUTEUR, ACCOMPAGNANT, AUTRE
    - `insuranceId` — `String`
    - `partnerId` — `String`
    - `portalLogin` — `String`
    - `mergedIntoId` — `String`
    - `deleted` — `boolean`
    - `createdAt` — `Instant`
    - `updatedAt` — `Instant`
    - `version` — `int`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>POST /api/v1/patients</code></summary>

- **Permission:** `patient.create`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Headers:** `Idempotency-Key` (optional)
- **Request body:** `CreatePatientRequest`
  - `name` — `NameRequest`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `maidenName` — `String`
  - `civility` — `Civility` — one of: MONSIEUR, MADAME, MADEMOISELLE, ENFANT, NOUVEAU_NE
  - `sex` — `Sex` — one of: M, F
  - `birthDate` — `LocalDate`
  - `idDocument` — `IdDocumentRequest`
    - `type` — `org.sparkcorporation.lims.patient.domain.IdDocumentType` — one of: CARTE_IDENTITE, PASSEPORT, PERMIS_CONDUIRE, AUTRE
    - `number` — `String`
  - `profession` — `String`
  - `address` — `String`
  - `clinicalNotes` — `String`
  - `contact` — `ContactRequest`
    - `phone` — `String`
    - `email` — `String`
  - `guardian` — `GuardianRequest`
    - `name` — `String`
    - `phone` — `String`
    - `relationship` — `org.sparkcorporation.lims.patient.domain.GuardianRelationship` — one of: PARENT, TUTEUR, ACCOMPAGNANT, AUTRE
  - `insuranceId` — `String`
  - `partnerId` — `String`
  - `portalLogin` — `String`
  - `portalPassword` — `String`
- **Response body:** `PatientResponse`
  - `id` — `String`
  - `labCode` — `String`
  - `name` — `Name`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `maidenName` — `String`
  - `civility` — `Civility` — one of: MONSIEUR, MADAME, MADEMOISELLE, ENFANT, NOUVEAU_NE
  - `sex` — `Sex` — one of: M, F
  - `birthDate` — `LocalDate`
  - `idDocument` — `IdDoc`
    - `type` — `IdDocumentType` — one of: CARTE_IDENTITE, PASSEPORT, PERMIS_CONDUIRE, AUTRE
    - `number` — `String`
  - `profession` — `String`
  - `address` — `String`
  - `clinicalNotes` — `String`
  - `contact` — `Contact`
    - `phone` — `String`
    - `email` — `String`
  - `guardian` — `Guardian`
    - `name` — `String`
    - `phone` — `String`
    - `relationship` — `GuardianRelationship` — one of: PARENT, TUTEUR, ACCOMPAGNANT, AUTRE
  - `insuranceId` — `String`
  - `partnerId` — `String`
  - `portalLogin` — `String`
  - `mergedIntoId` — `String`
  - `deleted` — `boolean`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>DELETE /api/v1/patients/{id}</code></summary>

- **Permission:** `patient.update`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/patients/{id}</code></summary>

- **Permission:** `patient.search`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `PatientResponse`
  - `id` — `String`
  - `labCode` — `String`
  - `name` — `Name`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `maidenName` — `String`
  - `civility` — `Civility` — one of: MONSIEUR, MADAME, MADEMOISELLE, ENFANT, NOUVEAU_NE
  - `sex` — `Sex` — one of: M, F
  - `birthDate` — `LocalDate`
  - `idDocument` — `IdDoc`
    - `type` — `IdDocumentType` — one of: CARTE_IDENTITE, PASSEPORT, PERMIS_CONDUIRE, AUTRE
    - `number` — `String`
  - `profession` — `String`
  - `address` — `String`
  - `clinicalNotes` — `String`
  - `contact` — `Contact`
    - `phone` — `String`
    - `email` — `String`
  - `guardian` — `Guardian`
    - `name` — `String`
    - `phone` — `String`
    - `relationship` — `GuardianRelationship` — one of: PARENT, TUTEUR, ACCOMPAGNANT, AUTRE
  - `insuranceId` — `String`
  - `partnerId` — `String`
  - `portalLogin` — `String`
  - `mergedIntoId` — `String`
  - `deleted` — `boolean`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>PATCH /api/v1/patients/{id}</code></summary>

- **Permission:** `patient.update`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `UpdatePatientRequest`
  - `name` — `CreatePatientRequest.NameRequest`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `maidenName` — `String`
  - `civility` — `Civility` — one of: MONSIEUR, MADAME, MADEMOISELLE, ENFANT, NOUVEAU_NE
  - `sex` — `Sex` — one of: M, F
  - `birthDate` — `LocalDate`
  - `idDocument` — `CreatePatientRequest.IdDocumentRequest`
    - `type` — `org.sparkcorporation.lims.patient.domain.IdDocumentType` — one of: CARTE_IDENTITE, PASSEPORT, PERMIS_CONDUIRE, AUTRE
    - `number` — `String`
  - `profession` — `String`
  - `address` — `String`
  - `clinicalNotes` — `String`
  - `contact` — `CreatePatientRequest.ContactRequest`
    - `phone` — `String`
    - `email` — `String`
  - `guardian` — `CreatePatientRequest.GuardianRequest`
    - `name` — `String`
    - `phone` — `String`
    - `relationship` — `org.sparkcorporation.lims.patient.domain.GuardianRelationship` — one of: PARENT, TUTEUR, ACCOMPAGNANT, AUTRE
  - `insuranceId` — `String`
  - `partnerId` — `String`
  - `portalLogin` — `String`
  - `portalPassword` — `String`
- **Response body:** `PatientResponse`
  - `id` — `String`
  - `labCode` — `String`
  - `name` — `Name`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `maidenName` — `String`
  - `civility` — `Civility` — one of: MONSIEUR, MADAME, MADEMOISELLE, ENFANT, NOUVEAU_NE
  - `sex` — `Sex` — one of: M, F
  - `birthDate` — `LocalDate`
  - `idDocument` — `IdDoc`
    - `type` — `IdDocumentType` — one of: CARTE_IDENTITE, PASSEPORT, PERMIS_CONDUIRE, AUTRE
    - `number` — `String`
  - `profession` — `String`
  - `address` — `String`
  - `clinicalNotes` — `String`
  - `contact` — `Contact`
    - `phone` — `String`
    - `email` — `String`
  - `guardian` — `Guardian`
    - `name` — `String`
    - `phone` — `String`
    - `relationship` — `GuardianRelationship` — one of: PARENT, TUTEUR, ACCOMPAGNANT, AUTRE
  - `insuranceId` — `String`
  - `partnerId` — `String`
  - `portalLogin` — `String`
  - `mergedIntoId` — `String`
  - `deleted` — `boolean`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>POST /api/v1/patients/{id}/merge</code></summary>

- **Permission:** `patient.merge`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `MergePatientRequest`
  - `targetId` — `String`
- **Response body:** `PatientResponse`
  - `id` — `String`
  - `labCode` — `String`
  - `name` — `Name`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `maidenName` — `String`
  - `civility` — `Civility` — one of: MONSIEUR, MADAME, MADEMOISELLE, ENFANT, NOUVEAU_NE
  - `sex` — `Sex` — one of: M, F
  - `birthDate` — `LocalDate`
  - `idDocument` — `IdDoc`
    - `type` — `IdDocumentType` — one of: CARTE_IDENTITE, PASSEPORT, PERMIS_CONDUIRE, AUTRE
    - `number` — `String`
  - `profession` — `String`
  - `address` — `String`
  - `clinicalNotes` — `String`
  - `contact` — `Contact`
    - `phone` — `String`
    - `email` — `String`
  - `guardian` — `Guardian`
    - `name` — `String`
    - `phone` — `String`
    - `relationship` — `GuardianRelationship` — one of: PARENT, TUTEUR, ACCOMPAGNANT, AUTRE
  - `insuranceId` — `String`
  - `partnerId` — `String`
  - `portalLogin` — `String`
  - `mergedIntoId` — `String`
  - `deleted` — `boolean`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>


### Catalog & referentials

| Method | Path | Permission | Success |
|---|---|---|---|
| `GET` | `/api/v1/catalog/analyses` | `catalog.manage` | 200 OK |
| `POST` | `/api/v1/catalog/analyses` | `catalog.manage` | 201 Created |
| `DELETE` | `/api/v1/catalog/analyses/{id}` | `catalog.manage` | 204 No Content |
| `GET` | `/api/v1/catalog/analyses/{id}` | `catalog.manage` | 200 OK |
| `PATCH` | `/api/v1/catalog/analyses/{id}` | `catalog.manage` | 200 OK |
| `POST` | `/api/v1/catalog/analyses/{id}/components` | `catalog.manage` | 201 Created |
| `DELETE` | `/api/v1/catalog/analyses/{id}/components/{childCode}` | `catalog.manage` | 204 No Content |
| `GET` | `/api/v1/catalog/analyses/{id}/history` | `catalog.view-history` | 200 OK |
| `PUT` | `/api/v1/catalog/analyses/{id}/intervals` | `catalog.manage` | 200 OK |
| `POST` | `/api/v1/catalog/analyses/{id}/intervals:replicate` | `catalog.manage` | 204 No Content |
| `PUT` | `/api/v1/catalog/analyses/{id}/techniques` | `catalog.manage` | 200 OK |
| `POST` | `/api/v1/catalog/analyses/{id}:convert-to-grouped` | `catalog.manage` | 200 OK |
| `POST` | `/api/v1/catalog/analyses/{id}:convert-to-simple` | `catalog.manage` | 200 OK |
| `POST` | `/api/v1/catalog/analyses/{id}:duplicate` | `catalog.manage` | 201 Created |
| `GET` | `/api/v1/catalog/antibiotics` | `catalog.manage` | 200 OK |
| `POST` | `/api/v1/catalog/antibiotics` | `catalog.manage` | 201 Created |
| `DELETE` | `/api/v1/catalog/antibiotics/{id}` | `catalog.manage` | 204 No Content |
| `GET` | `/api/v1/catalog/antibiotics/{id}` | `catalog.manage` | 200 OK |
| `PATCH` | `/api/v1/catalog/antibiotics/{id}` | `catalog.manage` | 200 OK |
| `GET` | `/api/v1/catalog/disciplines` | `catalog.manage` | 200 OK |
| `POST` | `/api/v1/catalog/disciplines` | `catalog.manage` | 201 Created |
| `DELETE` | `/api/v1/catalog/disciplines/{id}` | `catalog.manage` | 204 No Content |
| `GET` | `/api/v1/catalog/disciplines/{id}` | `catalog.manage` | 200 OK |
| `PATCH` | `/api/v1/catalog/disciplines/{id}` | `catalog.manage` | 200 OK |
| `GET` | `/api/v1/catalog/matrices` | `catalog.manage` | 200 OK |
| `POST` | `/api/v1/catalog/matrices` | `catalog.manage` | 201 Created |
| `DELETE` | `/api/v1/catalog/matrices/{id}` | `catalog.manage` | 204 No Content |
| `GET` | `/api/v1/catalog/matrices/{id}` | `catalog.manage` | 200 OK |
| `PATCH` | `/api/v1/catalog/matrices/{id}` | `catalog.manage` | 200 OK |
| `GET` | `/api/v1/catalog/preleveurs` | `catalog.manage` | 200 OK |
| `POST` | `/api/v1/catalog/preleveurs` | `catalog.manage` | 201 Created |
| `DELETE` | `/api/v1/catalog/preleveurs/{id}` | `catalog.manage` | 204 No Content |
| `GET` | `/api/v1/catalog/preleveurs/{id}` | `catalog.manage` | 200 OK |
| `PATCH` | `/api/v1/catalog/preleveurs/{id}` | `catalog.manage` | 200 OK |
| `GET` | `/api/v1/catalog/prescribers` | `prescriber.manage` | 200 OK |
| `POST` | `/api/v1/catalog/prescribers` | `prescriber.manage` | 201 Created |
| `DELETE` | `/api/v1/catalog/prescribers/{id}` | `prescriber.manage` | 204 No Content |
| `GET` | `/api/v1/catalog/prescribers/{id}` | `prescriber.manage` | 200 OK |
| `PATCH` | `/api/v1/catalog/prescribers/{id}` | `prescriber.manage` | 200 OK |
| `GET` | `/api/v1/catalog/recipients` | `catalog.manage` | 200 OK |
| `POST` | `/api/v1/catalog/recipients` | `catalog.manage` | 201 Created |
| `DELETE` | `/api/v1/catalog/recipients/{id}` | `catalog.manage` | 204 No Content |
| `GET` | `/api/v1/catalog/recipients/{id}` | `catalog.manage` | 200 OK |
| `PATCH` | `/api/v1/catalog/recipients/{id}` | `catalog.manage` | 200 OK |
| `GET` | `/api/v1/catalog/specialties` | `prescriber.manage` | 200 OK |
| `POST` | `/api/v1/catalog/specialties` | `prescriber.manage` | 201 Created |
| `DELETE` | `/api/v1/catalog/specialties/{id}` | `prescriber.manage` | 204 No Content |
| `GET` | `/api/v1/catalog/specialties/{id}` | `prescriber.manage` | 200 OK |
| `PATCH` | `/api/v1/catalog/specialties/{id}` | `prescriber.manage` | 200 OK |
| `GET` | `/api/v1/catalog/techniques` | `catalog.manage` | 200 OK |
| `POST` | `/api/v1/catalog/techniques` | `catalog.manage` | 201 Created |
| `DELETE` | `/api/v1/catalog/techniques/{id}` | `catalog.manage` | 204 No Content |
| `GET` | `/api/v1/catalog/techniques/{id}` | `catalog.manage` | 200 OK |
| `PATCH` | `/api/v1/catalog/techniques/{id}` | `catalog.manage` | 200 OK |
| `GET` | `/api/v1/insurances` | `lab-file.read` | 200 OK |
| `POST` | `/api/v1/insurances` | `insurance.manage` | 201 Created |
| `DELETE` | `/api/v1/insurances/{id}` | `insurance.manage` | 204 No Content |
| `GET` | `/api/v1/insurances/{id}` | `lab-file.read` | 200 OK |
| `PUT` | `/api/v1/insurances/{id}` | `insurance.manage` | 200 OK |
| `GET` | `/api/v1/partners` | `lab-file.read` | 200 OK |
| `POST` | `/api/v1/partners` | `partner.manage` | 201 Created |
| `DELETE` | `/api/v1/partners/{id}` | `partner.manage` | 204 No Content |
| `GET` | `/api/v1/partners/{id}` | `lab-file.read` | 200 OK |
| `PUT` | `/api/v1/partners/{id}` | `partner.manage` | 200 OK |

<details>
<summary><code>GET /api/v1/catalog/analyses</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Query params:**
  - `q` — `String`, optional
  - `disciplineCode` — `String`, optional
  - `kind` — `AnalysisKind`, optional — one of: SIMPLE, GROUPED, SOCIETY_PANEL
- **Response body:** `AnalysisSearchResponse`
  - `items` — `List<AnalysisResponse>`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
    - `kind` — `AnalysisKind` — one of: SIMPLE, GROUPED, SOCIETY_PANEL
    - `indiciaryValueB` — `BigDecimal`
    - `labelCount` — `int`
    - `discipline` — `DisciplineSnapshotResponse`
      - `id` — `String`
      - `code` — `String`
      - `name` — `String`
    - `matrix` — `MatrixSnapshotResponse`
      - `id` — `String`
      - `code` — `String`
      - `name` — `String`
    - `recipient` — `RecipientSnapshotResponse`
      - `id` — `String`
      - `code` — `String`
      - `name` — `String`
    - `resultType` — `ResultType` — one of: FORMULA, INTEGER, REAL, TEXT, FREE
    - `interpretationText` — `String`
    - `formulaExpression` — `String`
    - `defaultPortoir` — `String`
    - `defaultConservationDays` — `Integer`
    - `units` — `UnitsResponse`
      - `unit1` — `String`
      - `unit1Decimals` — `Integer`
      - `unit2` — `String`
      - `unit2Decimals` — `Integer`
      - `conversionFactor` — `BigDecimal`
    - `delay` — `AnalyticalDelayResponse`
      - `days` — `Integer`
      - `hours` — `Integer`
      - `anteriorityCount` — `Integer`
    - `components` — `List<ComponentLinkResponse>`
      - `childId` — `String`
      - `childCode` — `String`
      - `childName` — `String`
      - `position` — `int`
    - `techniques` — `List<TechniqueLinkResponse>`
      - `techniqueId` — `String`
      - `code` — `String`
      - `name` — `String`
      - `isDefault` — `boolean`
    - `createdAt` — `Instant`
    - `updatedAt` — `Instant`
    - `version` — `long`

</details>

<details>
<summary><code>POST /api/v1/catalog/analyses</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `CreateAnalysisRequest`
  - `code` — `String`
  - `name` — `String`
  - `kind` — `AnalysisKind` — one of: SIMPLE, GROUPED, SOCIETY_PANEL
  - `indiciaryValueB` — `BigDecimal`
  - `labelCount` — `Integer`
  - `disciplineId` — `String`
  - `matrixId` — `String`
  - `recipientId` — `String`
  - `resultType` — `ResultType` — one of: FORMULA, INTEGER, REAL, TEXT, FREE
  - `interpretationText` — `String`
  - `formulaExpression` — `String`
  - `defaultPortoir` — `String`
  - `defaultConservationDays` — `Integer`
  - `units` — `UnitsRequest`
    - `unit1` — `String`
    - `unit1Decimals` — `Integer`
    - `unit2` — `String`
    - `unit2Decimals` — `Integer`
    - `conversionFactor` — `BigDecimal`
  - `delay` — `AnalyticalDelayRequest`
    - `days` — `Integer`
    - `hours` — `Integer`
    - `anteriorityCount` — `Integer`
- **Response body:** `AnalysisResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `kind` — `AnalysisKind` — one of: SIMPLE, GROUPED, SOCIETY_PANEL
  - `indiciaryValueB` — `BigDecimal`
  - `labelCount` — `int`
  - `discipline` — `DisciplineSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `matrix` — `MatrixSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `recipient` — `RecipientSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `resultType` — `ResultType` — one of: FORMULA, INTEGER, REAL, TEXT, FREE
  - `interpretationText` — `String`
  - `formulaExpression` — `String`
  - `defaultPortoir` — `String`
  - `defaultConservationDays` — `Integer`
  - `units` — `UnitsResponse`
    - `unit1` — `String`
    - `unit1Decimals` — `Integer`
    - `unit2` — `String`
    - `unit2Decimals` — `Integer`
    - `conversionFactor` — `BigDecimal`
  - `delay` — `AnalyticalDelayResponse`
    - `days` — `Integer`
    - `hours` — `Integer`
    - `anteriorityCount` — `Integer`
  - `components` — `List<ComponentLinkResponse>`
    - `childId` — `String`
    - `childCode` — `String`
    - `childName` — `String`
    - `position` — `int`
  - `techniques` — `List<TechniqueLinkResponse>`
    - `techniqueId` — `String`
    - `code` — `String`
    - `name` — `String`
    - `isDefault` — `boolean`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>DELETE /api/v1/catalog/analyses/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/catalog/analyses/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `AnalysisResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `kind` — `AnalysisKind` — one of: SIMPLE, GROUPED, SOCIETY_PANEL
  - `indiciaryValueB` — `BigDecimal`
  - `labelCount` — `int`
  - `discipline` — `DisciplineSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `matrix` — `MatrixSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `recipient` — `RecipientSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `resultType` — `ResultType` — one of: FORMULA, INTEGER, REAL, TEXT, FREE
  - `interpretationText` — `String`
  - `formulaExpression` — `String`
  - `defaultPortoir` — `String`
  - `defaultConservationDays` — `Integer`
  - `units` — `UnitsResponse`
    - `unit1` — `String`
    - `unit1Decimals` — `Integer`
    - `unit2` — `String`
    - `unit2Decimals` — `Integer`
    - `conversionFactor` — `BigDecimal`
  - `delay` — `AnalyticalDelayResponse`
    - `days` — `Integer`
    - `hours` — `Integer`
    - `anteriorityCount` — `Integer`
  - `components` — `List<ComponentLinkResponse>`
    - `childId` — `String`
    - `childCode` — `String`
    - `childName` — `String`
    - `position` — `int`
  - `techniques` — `List<TechniqueLinkResponse>`
    - `techniqueId` — `String`
    - `code` — `String`
    - `name` — `String`
    - `isDefault` — `boolean`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>PATCH /api/v1/catalog/analyses/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `Map<String, Object>`
- **Response body:** `AnalysisResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `kind` — `AnalysisKind` — one of: SIMPLE, GROUPED, SOCIETY_PANEL
  - `indiciaryValueB` — `BigDecimal`
  - `labelCount` — `int`
  - `discipline` — `DisciplineSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `matrix` — `MatrixSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `recipient` — `RecipientSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `resultType` — `ResultType` — one of: FORMULA, INTEGER, REAL, TEXT, FREE
  - `interpretationText` — `String`
  - `formulaExpression` — `String`
  - `defaultPortoir` — `String`
  - `defaultConservationDays` — `Integer`
  - `units` — `UnitsResponse`
    - `unit1` — `String`
    - `unit1Decimals` — `Integer`
    - `unit2` — `String`
    - `unit2Decimals` — `Integer`
    - `conversionFactor` — `BigDecimal`
  - `delay` — `AnalyticalDelayResponse`
    - `days` — `Integer`
    - `hours` — `Integer`
    - `anteriorityCount` — `Integer`
  - `components` — `List<ComponentLinkResponse>`
    - `childId` — `String`
    - `childCode` — `String`
    - `childName` — `String`
    - `position` — `int`
  - `techniques` — `List<TechniqueLinkResponse>`
    - `techniqueId` — `String`
    - `code` — `String`
    - `name` — `String`
    - `isDefault` — `boolean`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>POST /api/v1/catalog/analyses/{id}/components</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 201 Created
- **Path params:** `id` (`String`)
- **Request body:** `AddComponentRequest`
  - `childCode` — `String`
  - `position` — `int`
- **Response body:** `AnalysisResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `kind` — `AnalysisKind` — one of: SIMPLE, GROUPED, SOCIETY_PANEL
  - `indiciaryValueB` — `BigDecimal`
  - `labelCount` — `int`
  - `discipline` — `DisciplineSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `matrix` — `MatrixSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `recipient` — `RecipientSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `resultType` — `ResultType` — one of: FORMULA, INTEGER, REAL, TEXT, FREE
  - `interpretationText` — `String`
  - `formulaExpression` — `String`
  - `defaultPortoir` — `String`
  - `defaultConservationDays` — `Integer`
  - `units` — `UnitsResponse`
    - `unit1` — `String`
    - `unit1Decimals` — `Integer`
    - `unit2` — `String`
    - `unit2Decimals` — `Integer`
    - `conversionFactor` — `BigDecimal`
  - `delay` — `AnalyticalDelayResponse`
    - `days` — `Integer`
    - `hours` — `Integer`
    - `anteriorityCount` — `Integer`
  - `components` — `List<ComponentLinkResponse>`
    - `childId` — `String`
    - `childCode` — `String`
    - `childName` — `String`
    - `position` — `int`
  - `techniques` — `List<TechniqueLinkResponse>`
    - `techniqueId` — `String`
    - `code` — `String`
    - `name` — `String`
    - `isDefault` — `boolean`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>DELETE /api/v1/catalog/analyses/{id}/components/{childCode}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`), `childCode` (`String`)

</details>

<details>
<summary><code>GET /api/v1/catalog/analyses/{id}/history</code></summary>

- **Permission:** `catalog.view-history`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Query params:**
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `AnalysisHistoryResponse`
  - `content` — `List<AnalysisHistoryEntry>`
    - `revision` — `long`
    - `timestamp` — `Instant`
    - `userId` — `String`
    - `scope` — `Scope` — one of: ANALYSIS, REFERENCE_INTERVALS, COMPONENTS, TECHNIQUES
    - `changedFields` — `List<String>`
    - `snapshot` — `Object`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>PUT /api/v1/catalog/analyses/{id}/intervals</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `PutIntervalsRequest`
  - `items` — `List< IntervalPayload>`
    - `category` — `PatientCategory` — one of: HOMME, FEMME, ENFANT, NOURRISSON_NOUVEAU_NE
    - `sex` — `Sex` — one of: M, F
    - `ageMin` — `Integer`
    - `ageMax` — `Integer`
    - `ageUnit` — `AgeUnit` — one of: DAYS, MONTHS, YEARS
    - `mode` — `ReferenceMode` — one of: STANDARD, COMMENT, NONE
    - `unit1Min` — `BigDecimal`
    - `unit1Max` — `BigDecimal`
    - `unit2Min` — `BigDecimal`
    - `unit2Max` — `BigDecimal`
    - `commentText` — `String`
- **Response body:** `List<ReferenceIntervalResponse>`
  - `id` — `String`
  - `analysisId` — `String`
  - `category` — `PatientCategory` — one of: HOMME, FEMME, ENFANT, NOURRISSON_NOUVEAU_NE
  - `sex` — `Sex` — one of: M, F
  - `ageMin` — `Integer`
  - `ageMax` — `Integer`
  - `ageUnit` — `AgeUnit` — one of: DAYS, MONTHS, YEARS
  - `mode` — `ReferenceMode` — one of: STANDARD, COMMENT, NONE
  - `unit1Min` — `BigDecimal`
  - `unit1Max` — `BigDecimal`
  - `unit2Min` — `BigDecimal`
  - `unit2Max` — `BigDecimal`
  - `commentText` — `String`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>POST /api/v1/catalog/analyses/{id}/intervals:replicate</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)
- **Request body:** `ReplicateIntervalsRequest`
  - `sourceCategory` — `PatientCategory` — one of: HOMME, FEMME, ENFANT, NOURRISSON_NOUVEAU_NE
  - `targetCategories` — `Set<PatientCategory>` — one of: HOMME, FEMME, ENFANT, NOURRISSON_NOUVEAU_NE

</details>

<details>
<summary><code>PUT /api/v1/catalog/analyses/{id}/techniques</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `PutTechniquesRequest`
  - `items` — `List< TechniqueLinkPayload>`
    - `techniqueId` — `String`
    - `isDefault` — `boolean`
- **Response body:** `AnalysisResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `kind` — `AnalysisKind` — one of: SIMPLE, GROUPED, SOCIETY_PANEL
  - `indiciaryValueB` — `BigDecimal`
  - `labelCount` — `int`
  - `discipline` — `DisciplineSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `matrix` — `MatrixSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `recipient` — `RecipientSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `resultType` — `ResultType` — one of: FORMULA, INTEGER, REAL, TEXT, FREE
  - `interpretationText` — `String`
  - `formulaExpression` — `String`
  - `defaultPortoir` — `String`
  - `defaultConservationDays` — `Integer`
  - `units` — `UnitsResponse`
    - `unit1` — `String`
    - `unit1Decimals` — `Integer`
    - `unit2` — `String`
    - `unit2Decimals` — `Integer`
    - `conversionFactor` — `BigDecimal`
  - `delay` — `AnalyticalDelayResponse`
    - `days` — `Integer`
    - `hours` — `Integer`
    - `anteriorityCount` — `Integer`
  - `components` — `List<ComponentLinkResponse>`
    - `childId` — `String`
    - `childCode` — `String`
    - `childName` — `String`
    - `position` — `int`
  - `techniques` — `List<TechniqueLinkResponse>`
    - `techniqueId` — `String`
    - `code` — `String`
    - `name` — `String`
    - `isDefault` — `boolean`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>POST /api/v1/catalog/analyses/{id}:convert-to-grouped</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `AnalysisResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `kind` — `AnalysisKind` — one of: SIMPLE, GROUPED, SOCIETY_PANEL
  - `indiciaryValueB` — `BigDecimal`
  - `labelCount` — `int`
  - `discipline` — `DisciplineSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `matrix` — `MatrixSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `recipient` — `RecipientSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `resultType` — `ResultType` — one of: FORMULA, INTEGER, REAL, TEXT, FREE
  - `interpretationText` — `String`
  - `formulaExpression` — `String`
  - `defaultPortoir` — `String`
  - `defaultConservationDays` — `Integer`
  - `units` — `UnitsResponse`
    - `unit1` — `String`
    - `unit1Decimals` — `Integer`
    - `unit2` — `String`
    - `unit2Decimals` — `Integer`
    - `conversionFactor` — `BigDecimal`
  - `delay` — `AnalyticalDelayResponse`
    - `days` — `Integer`
    - `hours` — `Integer`
    - `anteriorityCount` — `Integer`
  - `components` — `List<ComponentLinkResponse>`
    - `childId` — `String`
    - `childCode` — `String`
    - `childName` — `String`
    - `position` — `int`
  - `techniques` — `List<TechniqueLinkResponse>`
    - `techniqueId` — `String`
    - `code` — `String`
    - `name` — `String`
    - `isDefault` — `boolean`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>POST /api/v1/catalog/analyses/{id}:convert-to-simple</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `AnalysisResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `kind` — `AnalysisKind` — one of: SIMPLE, GROUPED, SOCIETY_PANEL
  - `indiciaryValueB` — `BigDecimal`
  - `labelCount` — `int`
  - `discipline` — `DisciplineSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `matrix` — `MatrixSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `recipient` — `RecipientSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `resultType` — `ResultType` — one of: FORMULA, INTEGER, REAL, TEXT, FREE
  - `interpretationText` — `String`
  - `formulaExpression` — `String`
  - `defaultPortoir` — `String`
  - `defaultConservationDays` — `Integer`
  - `units` — `UnitsResponse`
    - `unit1` — `String`
    - `unit1Decimals` — `Integer`
    - `unit2` — `String`
    - `unit2Decimals` — `Integer`
    - `conversionFactor` — `BigDecimal`
  - `delay` — `AnalyticalDelayResponse`
    - `days` — `Integer`
    - `hours` — `Integer`
    - `anteriorityCount` — `Integer`
  - `components` — `List<ComponentLinkResponse>`
    - `childId` — `String`
    - `childCode` — `String`
    - `childName` — `String`
    - `position` — `int`
  - `techniques` — `List<TechniqueLinkResponse>`
    - `techniqueId` — `String`
    - `code` — `String`
    - `name` — `String`
    - `isDefault` — `boolean`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>POST /api/v1/catalog/analyses/{id}:duplicate</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Path params:** `id` (`String`)
- **Request body:** `DuplicateAnalysisRequest`
  - `newCode` — `String`
- **Response body:** `AnalysisResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `kind` — `AnalysisKind` — one of: SIMPLE, GROUPED, SOCIETY_PANEL
  - `indiciaryValueB` — `BigDecimal`
  - `labelCount` — `int`
  - `discipline` — `DisciplineSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `matrix` — `MatrixSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `recipient` — `RecipientSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `resultType` — `ResultType` — one of: FORMULA, INTEGER, REAL, TEXT, FREE
  - `interpretationText` — `String`
  - `formulaExpression` — `String`
  - `defaultPortoir` — `String`
  - `defaultConservationDays` — `Integer`
  - `units` — `UnitsResponse`
    - `unit1` — `String`
    - `unit1Decimals` — `Integer`
    - `unit2` — `String`
    - `unit2Decimals` — `Integer`
    - `conversionFactor` — `BigDecimal`
  - `delay` — `AnalyticalDelayResponse`
    - `days` — `Integer`
    - `hours` — `Integer`
    - `anteriorityCount` — `Integer`
  - `components` — `List<ComponentLinkResponse>`
    - `childId` — `String`
    - `childCode` — `String`
    - `childName` — `String`
    - `position` — `int`
  - `techniques` — `List<TechniqueLinkResponse>`
    - `techniqueId` — `String`
    - `code` — `String`
    - `name` — `String`
    - `isDefault` — `boolean`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>GET /api/v1/catalog/antibiotics</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Query params:**
  - `activeOnly` — `boolean`, optional, default `true`
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `AntibioticSearchResponse`
  - `content` — `List<AntibioticResponse>`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
    - `description` — `String`
    - `family` — `String`
    - `referenceDiameter` — `BigDecimal`
    - `intermediateLower` — `BigDecimal`
    - `intermediateUpper` — `BigDecimal`
    - `deletedAt` — `Instant`
    - `createdAt` — `Instant`
    - `updatedAt` — `Instant`
    - `version` — `int`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>POST /api/v1/catalog/antibiotics</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `CreateAntibioticRequest`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `family` — `String`
  - `referenceDiameter` — `BigDecimal`
  - `intermediateLower` — `BigDecimal`
  - `intermediateUpper` — `BigDecimal`
- **Response body:** `AntibioticResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `family` — `String`
  - `referenceDiameter` — `BigDecimal`
  - `intermediateLower` — `BigDecimal`
  - `intermediateUpper` — `BigDecimal`
  - `deletedAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>DELETE /api/v1/catalog/antibiotics/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/catalog/antibiotics/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `AntibioticResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `family` — `String`
  - `referenceDiameter` — `BigDecimal`
  - `intermediateLower` — `BigDecimal`
  - `intermediateUpper` — `BigDecimal`
  - `deletedAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>PATCH /api/v1/catalog/antibiotics/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `UpdateAntibioticRequest`
  - `name` — `String`
  - `description` — `String`
  - `family` — `String`
  - `referenceDiameter` — `BigDecimal`
  - `intermediateLower` — `BigDecimal`
  - `intermediateUpper` — `BigDecimal`
- **Response body:** `AntibioticResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `family` — `String`
  - `referenceDiameter` — `BigDecimal`
  - `intermediateLower` — `BigDecimal`
  - `intermediateUpper` — `BigDecimal`
  - `deletedAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>GET /api/v1/catalog/disciplines</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Query params:**
  - `activeOnly` — `boolean`, optional, default `true`
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `DisciplineSearchResponse`
  - `content` — `List<DisciplineResponse>`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
    - `description` — `String`
    - `prefix` — `DisciplinePrefix` — one of: BIOCHIMIE, HEMATOLOGIE, IMMUNO_SEROLOGIE, MICROBIOLOGIE, BIOLOGIE_MOLECULAIRE
    - `deletedAt` — `Instant`
    - `createdAt` — `Instant`
    - `updatedAt` — `Instant`
    - `version` — `int`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>POST /api/v1/catalog/disciplines</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `CreateDisciplineRequest`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `disciplinePrefix` — `DisciplinePrefix` — one of: BIOCHIMIE, HEMATOLOGIE, IMMUNO_SEROLOGIE, MICROBIOLOGIE, BIOLOGIE_MOLECULAIRE
- **Response body:** `DisciplineResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `prefix` — `DisciplinePrefix` — one of: BIOCHIMIE, HEMATOLOGIE, IMMUNO_SEROLOGIE, MICROBIOLOGIE, BIOLOGIE_MOLECULAIRE
  - `deletedAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>DELETE /api/v1/catalog/disciplines/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/catalog/disciplines/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `DisciplineResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `prefix` — `DisciplinePrefix` — one of: BIOCHIMIE, HEMATOLOGIE, IMMUNO_SEROLOGIE, MICROBIOLOGIE, BIOLOGIE_MOLECULAIRE
  - `deletedAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>PATCH /api/v1/catalog/disciplines/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `Map<String, Object>`
- **Response body:** `DisciplineResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `prefix` — `DisciplinePrefix` — one of: BIOCHIMIE, HEMATOLOGIE, IMMUNO_SEROLOGIE, MICROBIOLOGIE, BIOLOGIE_MOLECULAIRE
  - `deletedAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>GET /api/v1/catalog/matrices</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Query params:**
  - `activeOnly` — `boolean`, optional, default `true`
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `MatrixSearchResponse`
  - `content` — `List<MatrixResponse>`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
    - `description` — `String`
    - `deletedAt` — `Instant`
    - `createdAt` — `Instant`
    - `updatedAt` — `Instant`
    - `version` — `int`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>POST /api/v1/catalog/matrices</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `CreateMatrixRequest`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
- **Response body:** `MatrixResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `deletedAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>DELETE /api/v1/catalog/matrices/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/catalog/matrices/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `MatrixResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `deletedAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>PATCH /api/v1/catalog/matrices/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `UpdateMatrixRequest`
  - `name` — `String`
  - `description` — `String`
- **Response body:** `MatrixResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `deletedAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>GET /api/v1/catalog/preleveurs</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Response body:** `PreleveurSearchResponse`
  - `items` — `List<PreleveurResponse>`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
    - `phone` — `String`
    - `email` — `String`
    - `userId` — `String`
    - `habilitations` — `Set<Habilitation>` — one of: TOUT_LE_MONDE, ADULTES_VEINES_VISIBLES, ADULTES_VEINES_PROFONDES_OU_FINES, PERSONNES_EN_FORME, ENFANTS_VEINES_VISIBLES, ENFANTS_VEINES_FINES, NOURRISSON_NOUVEAU_NE
    - `createdAt` — `Instant`
    - `updatedAt` — `Instant`
    - `version` — `long`

</details>

<details>
<summary><code>POST /api/v1/catalog/preleveurs</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `CreatePreleveurRequest`
  - `name` — `String`
  - `phone` — `String`
  - `email` — `String`
  - `userId` — `String`
  - `habilitations` — `Set<Habilitation>` — one of: TOUT_LE_MONDE, ADULTES_VEINES_VISIBLES, ADULTES_VEINES_PROFONDES_OU_FINES, PERSONNES_EN_FORME, ENFANTS_VEINES_VISIBLES, ENFANTS_VEINES_FINES, NOURRISSON_NOUVEAU_NE
- **Response body:** `PreleveurResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `phone` — `String`
  - `email` — `String`
  - `userId` — `String`
  - `habilitations` — `Set<Habilitation>` — one of: TOUT_LE_MONDE, ADULTES_VEINES_VISIBLES, ADULTES_VEINES_PROFONDES_OU_FINES, PERSONNES_EN_FORME, ENFANTS_VEINES_VISIBLES, ENFANTS_VEINES_FINES, NOURRISSON_NOUVEAU_NE
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>DELETE /api/v1/catalog/preleveurs/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/catalog/preleveurs/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `PreleveurResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `phone` — `String`
  - `email` — `String`
  - `userId` — `String`
  - `habilitations` — `Set<Habilitation>` — one of: TOUT_LE_MONDE, ADULTES_VEINES_VISIBLES, ADULTES_VEINES_PROFONDES_OU_FINES, PERSONNES_EN_FORME, ENFANTS_VEINES_VISIBLES, ENFANTS_VEINES_FINES, NOURRISSON_NOUVEAU_NE
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>PATCH /api/v1/catalog/preleveurs/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `UpdatePreleveurRequest`
  - `name` — `String`
  - `phone` — `String`
  - `email` — `String`
  - `userId` — `String`
  - `habilitations` — `Set<Habilitation>` — one of: TOUT_LE_MONDE, ADULTES_VEINES_VISIBLES, ADULTES_VEINES_PROFONDES_OU_FINES, PERSONNES_EN_FORME, ENFANTS_VEINES_VISIBLES, ENFANTS_VEINES_FINES, NOURRISSON_NOUVEAU_NE
- **Response body:** `PreleveurResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `phone` — `String`
  - `email` — `String`
  - `userId` — `String`
  - `habilitations` — `Set<Habilitation>` — one of: TOUT_LE_MONDE, ADULTES_VEINES_VISIBLES, ADULTES_VEINES_PROFONDES_OU_FINES, PERSONNES_EN_FORME, ENFANTS_VEINES_VISIBLES, ENFANTS_VEINES_FINES, NOURRISSON_NOUVEAU_NE
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>GET /api/v1/catalog/prescribers</code></summary>

- **Permission:** `prescriber.manage`
- **Success:** 200 OK
- **Query params:**
  - `q` — `String`, optional
  - `specialty` — `String`, optional
- **Response body:** `PrescriberSearchResponse`
  - `items` — `List<PrescriberResponse>`
    - `id` — `String`
    - `code` — `String`
    - `title` — `String`
    - `name` — `String`
    - `specialty` — `SpecialtySnapshotResponse`
      - `id` — `String`
      - `code` — `String`
      - `name` — `String`
    - `phone` — `String`
    - `email` — `String`
    - `licenseNumber` — `String`
    - `institution` — `String`
    - `createdAt` — `Instant`
    - `updatedAt` — `Instant`
    - `version` — `long`

</details>

<details>
<summary><code>POST /api/v1/catalog/prescribers</code></summary>

- **Permission:** `prescriber.manage`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `CreatePrescriberRequest`
  - `title` — `String`
  - `name` — `String`
  - `specialtyId` — `String`
  - `phone` — `String`
  - `email` — `String`
  - `licenseNumber` — `String`
  - `institution` — `String`
- **Response body:** `PrescriberResponse`
  - `id` — `String`
  - `code` — `String`
  - `title` — `String`
  - `name` — `String`
  - `specialty` — `SpecialtySnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `phone` — `String`
  - `email` — `String`
  - `licenseNumber` — `String`
  - `institution` — `String`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>DELETE /api/v1/catalog/prescribers/{id}</code></summary>

- **Permission:** `prescriber.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/catalog/prescribers/{id}</code></summary>

- **Permission:** `prescriber.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `PrescriberResponse`
  - `id` — `String`
  - `code` — `String`
  - `title` — `String`
  - `name` — `String`
  - `specialty` — `SpecialtySnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `phone` — `String`
  - `email` — `String`
  - `licenseNumber` — `String`
  - `institution` — `String`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>PATCH /api/v1/catalog/prescribers/{id}</code></summary>

- **Permission:** `prescriber.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `UpdatePrescriberRequest`
  - `title` — `String`
  - `name` — `String`
  - `specialtyId` — `String`
  - `phone` — `String`
  - `email` — `String`
  - `licenseNumber` — `String`
  - `institution` — `String`
- **Response body:** `PrescriberResponse`
  - `id` — `String`
  - `code` — `String`
  - `title` — `String`
  - `name` — `String`
  - `specialty` — `SpecialtySnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
  - `phone` — `String`
  - `email` — `String`
  - `licenseNumber` — `String`
  - `institution` — `String`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>GET /api/v1/catalog/recipients</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Query params:**
  - `activeOnly` — `boolean`, optional, default `true`
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `RecipientSearchResponse`
  - `content` — `List<RecipientResponse>`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
    - `description` — `String`
    - `color` — `String`
    - `volumeMl` — `BigDecimal`
    - `defaultMatrix` — `MatrixSnapshot`
      - `id` — `MatrixId`
      - `code` — `String`
      - `name` — `String`
    - `deletedAt` — `Instant`
    - `createdAt` — `Instant`
    - `updatedAt` — `Instant`
    - `version` — `int`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>POST /api/v1/catalog/recipients</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `CreateRecipientRequest`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `color` — `String`
  - `volumeMl` — `BigDecimal`
  - `defaultMatrixId` — `String`
- **Response body:** `RecipientResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `color` — `String`
  - `volumeMl` — `BigDecimal`
  - `defaultMatrix` — `MatrixSnapshot`
    - `id` — `MatrixId`
      - `value` — `String`
    - `code` — `String`
    - `name` — `String`
  - `deletedAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>DELETE /api/v1/catalog/recipients/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/catalog/recipients/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `RecipientResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `color` — `String`
  - `volumeMl` — `BigDecimal`
  - `defaultMatrix` — `MatrixSnapshot`
    - `id` — `MatrixId`
      - `value` — `String`
    - `code` — `String`
    - `name` — `String`
  - `deletedAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>PATCH /api/v1/catalog/recipients/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `UpdateRecipientRequest`
  - `name` — `String`
  - `description` — `String`
  - `color` — `String`
  - `volumeMl` — `BigDecimal`
  - `defaultMatrixId` — `String`
- **Response body:** `RecipientResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `color` — `String`
  - `volumeMl` — `BigDecimal`
  - `defaultMatrix` — `MatrixSnapshot`
    - `id` — `MatrixId`
      - `value` — `String`
    - `code` — `String`
    - `name` — `String`
  - `deletedAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>GET /api/v1/catalog/specialties</code></summary>

- **Permission:** `prescriber.manage`
- **Success:** 200 OK
- **Query params:**
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `SpecialtySearchResponse`
  - `items` — `List<SpecialtyResponse>`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
    - `description` — `String`
    - `createdAt` — `Instant`
    - `updatedAt` — `Instant`
    - `version` — `long`

</details>

<details>
<summary><code>POST /api/v1/catalog/specialties</code></summary>

- **Permission:** `prescriber.manage`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `CreateSpecialtyRequest`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
- **Response body:** `SpecialtyResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>DELETE /api/v1/catalog/specialties/{id}</code></summary>

- **Permission:** `prescriber.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/catalog/specialties/{id}</code></summary>

- **Permission:** `prescriber.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `SpecialtyResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>PATCH /api/v1/catalog/specialties/{id}</code></summary>

- **Permission:** `prescriber.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `Map<String, Object>`
- **Response body:** `SpecialtyResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `long`

</details>

<details>
<summary><code>GET /api/v1/catalog/techniques</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Query params:**
  - `activeOnly` — `boolean`, optional, default `true`
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `TechniqueSearchResponse`
  - `content` — `List<TechniqueResponse>`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
    - `description` — `String`
    - `deletedAt` — `Instant`
    - `createdAt` — `Instant`
    - `updatedAt` — `Instant`
    - `version` — `int`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>POST /api/v1/catalog/techniques</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `CreateTechniqueRequest`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
- **Response body:** `TechniqueResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `deletedAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>DELETE /api/v1/catalog/techniques/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/catalog/techniques/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `TechniqueResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `deletedAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>PATCH /api/v1/catalog/techniques/{id}</code></summary>

- **Permission:** `catalog.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `UpdateTechniqueRequest`
  - `name` — `String`
  - `description` — `String`
- **Response body:** `TechniqueResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `description` — `String`
  - `deletedAt` — `Instant`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`
  - `version` — `int`

</details>

<details>
<summary><code>GET /api/v1/insurances</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Query params:**
  - `q` — `String`, optional
  - `disabled` — `Boolean`, optional
- **Response body:** `InsuranceSearchResponse`
  - `items` — `List<InsuranceResponse>`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
    - `responsibleEmail` — `String`
    - `responsibleContact` — `String`
    - `bRate` — `BigDecimal`
    - `coverageRate` — `BigDecimal`
    - `disabled` — `boolean`
  - `totalElements` — `long`

</details>

<details>
<summary><code>POST /api/v1/insurances</code></summary>

- **Permission:** `insurance.manage`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `InsuranceRequest.Create`
- **Response body:** `InsuranceResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `responsibleEmail` — `String`
  - `responsibleContact` — `String`
  - `bRate` — `BigDecimal`
  - `coverageRate` — `BigDecimal`
  - `disabled` — `boolean`

</details>

<details>
<summary><code>DELETE /api/v1/insurances/{id}</code></summary>

- **Permission:** `insurance.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/insurances/{id}</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `InsuranceResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `responsibleEmail` — `String`
  - `responsibleContact` — `String`
  - `bRate` — `BigDecimal`
  - `coverageRate` — `BigDecimal`
  - `disabled` — `boolean`

</details>

<details>
<summary><code>PUT /api/v1/insurances/{id}</code></summary>

- **Permission:** `insurance.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `InsuranceRequest.Update`
- **Response body:** `InsuranceResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `responsibleEmail` — `String`
  - `responsibleContact` — `String`
  - `bRate` — `BigDecimal`
  - `coverageRate` — `BigDecimal`
  - `disabled` — `boolean`

</details>

<details>
<summary><code>GET /api/v1/partners</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Query params:**
  - `q` — `String`, optional
  - `kind` — `PartnerKind`, optional — one of: SOCIETY, PARTNER_LAB, HEALTH_CENTER
  - `disabled` — `Boolean`, optional
- **Response body:** `PartnerSearchResponse`
  - `items` — `List<PartnerResponse>`
    - `id` — `String`
    - `code` — `String`
    - `name` — `String`
    - `kind` — `PartnerKind` — one of: SOCIETY, PARTNER_LAB, HEALTH_CENTER
    - `responsibleEmail` — `String`
    - `responsibleContact` — `String`
    - `bRate` — `BigDecimal`
    - `disabled` — `boolean`
  - `totalElements` — `long`

</details>

<details>
<summary><code>POST /api/v1/partners</code></summary>

- **Permission:** `partner.manage`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `PartnerRequest.Create`
- **Response body:** `PartnerResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `kind` — `PartnerKind` — one of: SOCIETY, PARTNER_LAB, HEALTH_CENTER
  - `responsibleEmail` — `String`
  - `responsibleContact` — `String`
  - `bRate` — `BigDecimal`
  - `disabled` — `boolean`

</details>

<details>
<summary><code>DELETE /api/v1/partners/{id}</code></summary>

- **Permission:** `partner.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/partners/{id}</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `PartnerResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `kind` — `PartnerKind` — one of: SOCIETY, PARTNER_LAB, HEALTH_CENTER
  - `responsibleEmail` — `String`
  - `responsibleContact` — `String`
  - `bRate` — `BigDecimal`
  - `disabled` — `boolean`

</details>

<details>
<summary><code>PUT /api/v1/partners/{id}</code></summary>

- **Permission:** `partner.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `PartnerRequest.Update`
- **Response body:** `PartnerResponse`
  - `id` — `String`
  - `code` — `String`
  - `name` — `String`
  - `kind` — `PartnerKind` — one of: SOCIETY, PARTNER_LAB, HEALTH_CENTER
  - `responsibleEmail` — `String`
  - `responsibleContact` — `String`
  - `bRate` — `BigDecimal`
  - `disabled` — `boolean`

</details>


### Lab files, quotes & samples

| Method | Path | Permission | Success |
|---|---|---|---|
| `GET` | `/api/v1/lab-files` | `lab-file.read` | 200 OK |
| `POST` | `/api/v1/lab-files` | `lab-file.create` | 201 Created |
| `DELETE` | `/api/v1/lab-files/{id}` | `lab-file.create` | 204 No Content |
| `GET` | `/api/v1/lab-files/{id}` | `lab-file.read` | 200 OK |
| `PATCH` | `/api/v1/lab-files/{id}` | `lab-file.create` | 200 OK |
| `GET` | `/api/v1/lab-files/{id}/labels` | `lab-file.read` | 200 OK |
| `PUT` | `/api/v1/lab-files/{id}/rendezvous` | `lab-file.create` | 200 OK |
| `GET` | `/api/v1/lab-files/{id}/worksheet.pdf` | `lab-file.read` | 200 OK |
| `POST` | `/api/v1/lab-files/{labFileId}/analyses` | `lab-file.create` | 200 OK |
| `DELETE` | `/api/v1/lab-files/{labFileId}/analyses/{requestId}` | `lab-file.create` | 204 No Content |
| `GET` | `/api/v1/lab-files/{labFileId}/attachments` | `lab-file.read` | 200 OK |
| `POST` | `/api/v1/lab-files/{labFileId}/attachments` | `lab-file.create` | 201 Created |
| `DELETE` | `/api/v1/lab-files/{labFileId}/attachments/{attachmentId}` | `lab-file.create` | 204 No Content |
| `GET` | `/api/v1/lab-files/{labFileId}/attachments/{attachmentId}` | `lab-file.read` | 200 OK |
| `POST` | `/api/v1/lab-files/{labFileId}/samples:external` | `sample.record-external` | 201 Created |
| `POST` | `/api/v1/lab-files/{labFileId}/samples:home` | `sample.record-home` | 201 Created |
| `POST` | `/api/v1/lab-files/{labFileId}/samples:in-lab` | `sample.record-in-lab` | 201 Created |
| `GET` | `/api/v1/quotes` | `quote.read` | 200 OK |
| `POST` | `/api/v1/quotes` | `quote.create` | 201 Created |
| `DELETE` | `/api/v1/quotes/{id}` | `quote.create` | 204 No Content |
| `GET` | `/api/v1/quotes/{id}` | `quote.read` | 200 OK |
| `PATCH` | `/api/v1/quotes/{id}` | `quote.create` | 200 OK |
| `GET` | `/api/v1/quotes/{id}/export` | `quote.read` | 200 OK |
| `POST` | `/api/v1/quotes/{id}/lines` | `quote.create` | 200 OK |
| `DELETE` | `/api/v1/quotes/{id}/lines/{lineId}` | `quote.create` | 204 No Content |
| `GET` | `/api/v1/quotes/{id}/pdf` | `quote.read` | 200 OK |
| `POST` | `/api/v1/quotes/{id}:convert` | `quote.create` | 201 Created |
| `POST` | `/api/v1/quotes/{id}:lock` | `quote.lock` | 200 OK |
| `POST` | `/api/v1/quotes/{id}:send` | `quote.send` | 202 Accepted |
| `GET` | `/api/v1/samples/queues/destruction-due` | `lab-file.read` | 200 OK |
| `POST` | `/api/v1/samples/{id}:mark-destroyed` | `sample.destroy` | 204 No Content |

<details>
<summary><code>GET /api/v1/lab-files</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Query params:**
  - `patientId` — `String`, optional
  - `prescriberId` — `String`, optional
  - `urgent` — `Boolean`, optional
  - `status` — `LabFileStatus`, optional — one of: OPEN, AWAITING_RESULTS, TECHNICALLY_VALIDATED, BIOLOGICALLY_VALIDATED, SIGNED, PUBLISHED
  - `from` — `Instant`, optional
  - `to` — `Instant`, optional
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `LabFileSearchResponse`
  - `content` — `List<LabFileResponse>`
    - `id` — `String`
    - `registrationNumber` — `String`
    - `patient` — `PatientSnapshotResponse`
      - `id` — `String`
      - `labCode` — `String`
      - `lastName` — `String`
      - `firstNames` — `String`
      - `sex` — `Sex` — one of: M, F
    - `prescriber` — `PrescriberSnapshotResponse`
      - `id` — `String`
      - `code` — `String`
      - `fullName` — `String`
      - `specialtyCode` — `String`
    - `preleveur` — `PreleveurSnapshotResponse`
      - `id` — `String`
      - `code` — `String`
      - `fullName` — `String`
    - `urgent` — `boolean`
    - `status` — `LabFileStatus` — one of: OPEN, AWAITING_RESULTS, TECHNICALLY_VALIDATED, BIOLOGICALLY_VALIDATED, SIGNED, PUBLISHED
    - `samples` — `List<SampleResponse>`
    - `analyses` — `List<AnalysisRequestResponse>`
      - `id` — `String`
      - `analysisId` — `String`
      - `analysisCode` — `String`
      - `requestedAt` — `Instant`
    - `createdAt` — `Instant`
    - `createdByUserId` — `String`
    - `rendezVousAt` — `Instant`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>POST /api/v1/lab-files</code></summary>

- **Permission:** `lab-file.create`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `CreateLabFileRequest`
  - `patientId` — `String`
  - `prescriberId` — `String`
  - `preleveurId` — `String`
  - `urgent` — `boolean`
  - `analyses` — `List< String>`
  - `insuranceId` — `String`
  - `partnerId` — `String`
  - `homeCollectionFee` — `BigDecimal`
  - `additionalFees` — `BigDecimal`
  - `discountKind` — `DiscountKind` — one of: AMOUNT, PERCENT
  - `discountValue` — `BigDecimal`
  - `rendezVousAt` — `Instant`
- **Response body:** `LabFileResponse`
  - `id` — `String`
  - `registrationNumber` — `String`
  - `patient` — `PatientSnapshotResponse`
    - `id` — `String`
    - `labCode` — `String`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `sex` — `Sex` — one of: M, F
  - `prescriber` — `PrescriberSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
    - `specialtyCode` — `String`
  - `preleveur` — `PreleveurSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
  - `urgent` — `boolean`
  - `status` — `LabFileStatus` — one of: OPEN, AWAITING_RESULTS, TECHNICALLY_VALIDATED, BIOLOGICALLY_VALIDATED, SIGNED, PUBLISHED
  - `samples` — `List<SampleResponse>`
  - `analyses` — `List<AnalysisRequestResponse>`
    - `id` — `String`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `requestedAt` — `Instant`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`
  - `rendezVousAt` — `Instant`

</details>

<details>
<summary><code>DELETE /api/v1/lab-files/{id}</code></summary>

- **Permission:** `lab-file.create`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/lab-files/{id}</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `LabFileResponse`
  - `id` — `String`
  - `registrationNumber` — `String`
  - `patient` — `PatientSnapshotResponse`
    - `id` — `String`
    - `labCode` — `String`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `sex` — `Sex` — one of: M, F
  - `prescriber` — `PrescriberSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
    - `specialtyCode` — `String`
  - `preleveur` — `PreleveurSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
  - `urgent` — `boolean`
  - `status` — `LabFileStatus` — one of: OPEN, AWAITING_RESULTS, TECHNICALLY_VALIDATED, BIOLOGICALLY_VALIDATED, SIGNED, PUBLISHED
  - `samples` — `List<SampleResponse>`
  - `analyses` — `List<AnalysisRequestResponse>`
    - `id` — `String`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `requestedAt` — `Instant`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`
  - `rendezVousAt` — `Instant`

</details>

<details>
<summary><code>PATCH /api/v1/lab-files/{id}</code></summary>

- **Permission:** `lab-file.create`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `Map<String, Object>`
- **Response body:** `LabFileResponse`
  - `id` — `String`
  - `registrationNumber` — `String`
  - `patient` — `PatientSnapshotResponse`
    - `id` — `String`
    - `labCode` — `String`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `sex` — `Sex` — one of: M, F
  - `prescriber` — `PrescriberSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
    - `specialtyCode` — `String`
  - `preleveur` — `PreleveurSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
  - `urgent` — `boolean`
  - `status` — `LabFileStatus` — one of: OPEN, AWAITING_RESULTS, TECHNICALLY_VALIDATED, BIOLOGICALLY_VALIDATED, SIGNED, PUBLISHED
  - `samples` — `List<SampleResponse>`
  - `analyses` — `List<AnalysisRequestResponse>`
    - `id` — `String`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `requestedAt` — `Instant`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`
  - `rendezVousAt` — `Instant`

</details>

<details>
<summary><code>GET /api/v1/lab-files/{id}/labels</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `LabelResponse`
  - `labFileId` — `String`
  - `labels` — `List<LabelRecord>`
    - `sampleId` — `String`
    - `dailyId` — `String`
    - `discipline` — `String`
    - `patient` — `PatientBlock`
      - `nom` — `String`
      - `prenom` — `String`
      - `age` — `String`
      - `sexe` — `String`
    - `analyses` — `List<String>`
    - `copies` — `int`

</details>

<details>
<summary><code>PUT /api/v1/lab-files/{id}/rendezvous</code></summary>

- **Permission:** `lab-file.create`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `RendezVousRequest`
  - `rendezVousAt` — `Instant`
- **Response body:** `LabFileResponse`
  - `id` — `String`
  - `registrationNumber` — `String`
  - `patient` — `PatientSnapshotResponse`
    - `id` — `String`
    - `labCode` — `String`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `sex` — `Sex` — one of: M, F
  - `prescriber` — `PrescriberSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
    - `specialtyCode` — `String`
  - `preleveur` — `PreleveurSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
  - `urgent` — `boolean`
  - `status` — `LabFileStatus` — one of: OPEN, AWAITING_RESULTS, TECHNICALLY_VALIDATED, BIOLOGICALLY_VALIDATED, SIGNED, PUBLISHED
  - `samples` — `List<SampleResponse>`
  - `analyses` — `List<AnalysisRequestResponse>`
    - `id` — `String`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `requestedAt` — `Instant`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`
  - `rendezVousAt` — `Instant`

</details>

<details>
<summary><code>GET /api/v1/lab-files/{id}/worksheet.pdf</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Produces:** application/pdf
- **Download:** binary body with `Content-Disposition: attachment; filename="…"`
- **Path params:** `id` (`String`)
- **Response body:** `byte[]`

</details>

<details>
<summary><code>POST /api/v1/lab-files/{labFileId}/analyses</code></summary>

- **Permission:** `lab-file.create`
- **Success:** 200 OK
- **Path params:** `labFileId` (`String`)
- **Request body:** `AddAnalysesRequest`
  - `analyses` — `List< String>`
- **Response body:** `LabFileResponse`
  - `id` — `String`
  - `registrationNumber` — `String`
  - `patient` — `PatientSnapshotResponse`
    - `id` — `String`
    - `labCode` — `String`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `sex` — `Sex` — one of: M, F
  - `prescriber` — `PrescriberSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
    - `specialtyCode` — `String`
  - `preleveur` — `PreleveurSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
  - `urgent` — `boolean`
  - `status` — `LabFileStatus` — one of: OPEN, AWAITING_RESULTS, TECHNICALLY_VALIDATED, BIOLOGICALLY_VALIDATED, SIGNED, PUBLISHED
  - `samples` — `List<SampleResponse>`
  - `analyses` — `List<AnalysisRequestResponse>`
    - `id` — `String`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `requestedAt` — `Instant`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`
  - `rendezVousAt` — `Instant`

</details>

<details>
<summary><code>DELETE /api/v1/lab-files/{labFileId}/analyses/{requestId}</code></summary>

- **Permission:** `lab-file.create`
- **Success:** 204 No Content
- **Path params:** `labFileId` (`String`), `requestId` (`String`)

</details>

<details>
<summary><code>GET /api/v1/lab-files/{labFileId}/attachments</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Path params:** `labFileId` (`String`)
- **Response body:** `List<AttachmentSummary>`
  - `id` — `String`
  - `type` — `AttachableType` — one of: LAB_FILE, EXPENSE, HOME_COLLECTION_FEE
  - `targetId` — `String`
  - `filename` — `String`
  - `contentType` — `String`
  - `sizeBytes` — `long`
  - `sha256` — `String`
  - `storageUri` — `String`
  - `uploadedByUserId` — `String`
  - `uploadedAt` — `Instant`

</details>

<details>
<summary><code>POST /api/v1/lab-files/{labFileId}/attachments</code></summary>

- **Permission:** `lab-file.create`
- **Success:** 201 Created
- **Consumes:** `multipart/form-data`
- **Path params:** `labFileId` (`String`)
- **Multipart part `file`:** `MultipartFile`
- **Response body:** `AttachmentSummary`
  - `id` — `String`
  - `type` — `AttachableType` — one of: LAB_FILE, EXPENSE, HOME_COLLECTION_FEE
  - `targetId` — `String`
  - `filename` — `String`
  - `contentType` — `String`
  - `sizeBytes` — `long`
  - `sha256` — `String`
  - `storageUri` — `String`
  - `uploadedByUserId` — `String`
  - `uploadedAt` — `Instant`

</details>

<details>
<summary><code>DELETE /api/v1/lab-files/{labFileId}/attachments/{attachmentId}</code></summary>

- **Permission:** `lab-file.create`
- **Success:** 204 No Content
- **Path params:** `labFileId` (`String`), `attachmentId` (`String`)

</details>

<details>
<summary><code>GET /api/v1/lab-files/{labFileId}/attachments/{attachmentId}</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Produces:** varies — read the `Content-Type` response header
- **Download:** binary body with `Content-Disposition: attachment; filename="…"`
- **Path params:** `labFileId` (`String`), `attachmentId` (`String`)
- **Response body:** `byte[]`

</details>

<details>
<summary><code>POST /api/v1/lab-files/{labFileId}/samples:external</code></summary>

- **Permission:** `sample.record-external`
- **Success:** 201 Created
- **Path params:** `labFileId` (`String`)
- **Request body:** `RecordExternalSampleRequest`
  - `analysisId` — `String`
  - `recipientId` — `String`
  - `collectedAt` — `Instant`
  - `receivedAt` — `Instant`
  - `receivedByUserId` — `String`
  - `origin` — `String`
- **Response body:** `SampleResponse`

</details>

<details>
<summary><code>POST /api/v1/lab-files/{labFileId}/samples:home</code></summary>

- **Permission:** `sample.record-home`
- **Success:** 201 Created
- **Path params:** `labFileId` (`String`)
- **Request body:** `RecordHomeSampleRequest`
  - `analysisId` — `String`
  - `recipientId` — `String`
  - `collectedAt` — `Instant`
  - `addressLine` — `String`
  - `distanceKm` — `int`
  - `windowStart` — `Instant`
  - `windowEnd` — `Instant`
  - `preleveurId` — `String`
- **Response body:** `SampleResponse`

</details>

<details>
<summary><code>POST /api/v1/lab-files/{labFileId}/samples:in-lab</code></summary>

- **Permission:** `sample.record-in-lab`
- **Success:** 201 Created
- **Path params:** `labFileId` (`String`)
- **Request body:** `RecordInLabSampleRequest`
  - `analysisId` — `String`
  - `recipientId` — `String`
  - `collectedAt` — `Instant`
- **Response body:** `SampleResponse`

</details>

<details>
<summary><code>GET /api/v1/quotes</code></summary>

- **Permission:** `quote.read`
- **Success:** 200 OK
- **Query params:**
  - `patientId` — `String`, optional
  - `prescriberId` — `String`, optional
  - `status` — `QuoteStatus`, optional — one of: DRAFT, LOCKED, CONVERTED, EXPIRED
  - `from` — `Instant`, optional
  - `to` — `Instant`, optional
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `QuoteSearchResponse`
  - `content` — `List<QuoteResponse>`
    - `id` — `String`
    - `quoteNumber` — `String`
    - `patient` — `PatientSnapshotResponse`
      - `id` — `String`
      - `labCode` — `String`
      - `lastName` — `String`
      - `firstNames` — `String`
      - `sex` — `Sex` — one of: M, F
    - `prescriber` — `PrescriberSnapshotResponse`
      - `id` — `String`
      - `code` — `String`
      - `fullName` — `String`
      - `specialtyCode` — `String`
    - `status` — `QuoteStatus` — one of: DRAFT, LOCKED, CONVERTED, EXPIRED
    - `lockedAt` — `Instant`
    - `labFileId` — `String`
    - `convertedAt` — `Instant`
    - `expiresAt` — `Instant`
    - `totalAmount` — `BigDecimal`
    - `lines` — `List<QuoteLineResponse>`
      - `id` — `String`
      - `analysisId` — `String`
      - `analysisCode` — `String`
      - `indiciaryValueBSnapshot` — `BigDecimal`
      - `bRateSnapshot` — `BigDecimal`
      - `labelCountSnapshot` — `int`
      - `lineTotal` — `BigDecimal`
    - `createdAt` — `Instant`
    - `createdByUserId` — `String`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>POST /api/v1/quotes</code></summary>

- **Permission:** `quote.create`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `CreateQuoteRequest`
  - `patientId` — `String`
  - `prescriberId` — `String`
  - `analyses` — `List< String>`
  - `expiresAt` — `Instant`
- **Response body:** `QuoteResponse`
  - `id` — `String`
  - `quoteNumber` — `String`
  - `patient` — `PatientSnapshotResponse`
    - `id` — `String`
    - `labCode` — `String`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `sex` — `Sex` — one of: M, F
  - `prescriber` — `PrescriberSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
    - `specialtyCode` — `String`
  - `status` — `QuoteStatus` — one of: DRAFT, LOCKED, CONVERTED, EXPIRED
  - `lockedAt` — `Instant`
  - `labFileId` — `String`
  - `convertedAt` — `Instant`
  - `expiresAt` — `Instant`
  - `totalAmount` — `BigDecimal`
  - `lines` — `List<QuoteLineResponse>`
    - `id` — `String`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `indiciaryValueBSnapshot` — `BigDecimal`
    - `bRateSnapshot` — `BigDecimal`
    - `labelCountSnapshot` — `int`
    - `lineTotal` — `BigDecimal`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`

</details>

<details>
<summary><code>DELETE /api/v1/quotes/{id}</code></summary>

- **Permission:** `quote.create`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/quotes/{id}</code></summary>

- **Permission:** `quote.read`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `QuoteResponse`
  - `id` — `String`
  - `quoteNumber` — `String`
  - `patient` — `PatientSnapshotResponse`
    - `id` — `String`
    - `labCode` — `String`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `sex` — `Sex` — one of: M, F
  - `prescriber` — `PrescriberSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
    - `specialtyCode` — `String`
  - `status` — `QuoteStatus` — one of: DRAFT, LOCKED, CONVERTED, EXPIRED
  - `lockedAt` — `Instant`
  - `labFileId` — `String`
  - `convertedAt` — `Instant`
  - `expiresAt` — `Instant`
  - `totalAmount` — `BigDecimal`
  - `lines` — `List<QuoteLineResponse>`
    - `id` — `String`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `indiciaryValueBSnapshot` — `BigDecimal`
    - `bRateSnapshot` — `BigDecimal`
    - `labelCountSnapshot` — `int`
    - `lineTotal` — `BigDecimal`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`

</details>

<details>
<summary><code>PATCH /api/v1/quotes/{id}</code></summary>

- **Permission:** `quote.create`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `Map<String, Object>`
- **Response body:** `QuoteResponse`
  - `id` — `String`
  - `quoteNumber` — `String`
  - `patient` — `PatientSnapshotResponse`
    - `id` — `String`
    - `labCode` — `String`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `sex` — `Sex` — one of: M, F
  - `prescriber` — `PrescriberSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
    - `specialtyCode` — `String`
  - `status` — `QuoteStatus` — one of: DRAFT, LOCKED, CONVERTED, EXPIRED
  - `lockedAt` — `Instant`
  - `labFileId` — `String`
  - `convertedAt` — `Instant`
  - `expiresAt` — `Instant`
  - `totalAmount` — `BigDecimal`
  - `lines` — `List<QuoteLineResponse>`
    - `id` — `String`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `indiciaryValueBSnapshot` — `BigDecimal`
    - `bRateSnapshot` — `BigDecimal`
    - `labelCountSnapshot` — `int`
    - `lineTotal` — `BigDecimal`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`

</details>

<details>
<summary><code>GET /api/v1/quotes/{id}/export</code></summary>

- **Permission:** `quote.read`
- **Success:** 200 OK
- **Produces:** varies — read the `Content-Type` response header
- **Download:** binary body with `Content-Disposition: attachment; filename="…"`
- **Path params:** `id` (`String`)
- **Query params:**
  - `format` — `String`, required
- **Response body:** `byte[]`

</details>

<details>
<summary><code>POST /api/v1/quotes/{id}/lines</code></summary>

- **Permission:** `quote.create`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `AddQuoteLinesRequest`
  - `analyses` — `List< String>`
- **Response body:** `QuoteResponse`
  - `id` — `String`
  - `quoteNumber` — `String`
  - `patient` — `PatientSnapshotResponse`
    - `id` — `String`
    - `labCode` — `String`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `sex` — `Sex` — one of: M, F
  - `prescriber` — `PrescriberSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
    - `specialtyCode` — `String`
  - `status` — `QuoteStatus` — one of: DRAFT, LOCKED, CONVERTED, EXPIRED
  - `lockedAt` — `Instant`
  - `labFileId` — `String`
  - `convertedAt` — `Instant`
  - `expiresAt` — `Instant`
  - `totalAmount` — `BigDecimal`
  - `lines` — `List<QuoteLineResponse>`
    - `id` — `String`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `indiciaryValueBSnapshot` — `BigDecimal`
    - `bRateSnapshot` — `BigDecimal`
    - `labelCountSnapshot` — `int`
    - `lineTotal` — `BigDecimal`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`

</details>

<details>
<summary><code>DELETE /api/v1/quotes/{id}/lines/{lineId}</code></summary>

- **Permission:** `quote.create`
- **Success:** 204 No Content
- **Path params:** `id` (`String`), `lineId` (`String`)

</details>

<details>
<summary><code>GET /api/v1/quotes/{id}/pdf</code></summary>

- **Permission:** `quote.read`
- **Success:** 200 OK
- **Produces:** application/pdf
- **Download:** binary body with `Content-Disposition: attachment; filename="…"`
- **Path params:** `id` (`String`)
- **Response body:** `byte[]`

</details>

<details>
<summary><code>POST /api/v1/quotes/{id}:convert</code></summary>

- **Permission:** `quote.create`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Path params:** `id` (`String`)
- **Request body:** `ConvertQuoteRequest`
  - `urgent` — `boolean`
- **Response body:** `LabFileResponse`
  - `id` — `String`
  - `registrationNumber` — `String`
  - `patient` — `PatientSnapshotResponse`
    - `id` — `String`
    - `labCode` — `String`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `sex` — `Sex` — one of: M, F
  - `prescriber` — `PrescriberSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
    - `specialtyCode` — `String`
  - `preleveur` — `PreleveurSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
  - `urgent` — `boolean`
  - `status` — `LabFileStatus` — one of: OPEN, AWAITING_RESULTS, TECHNICALLY_VALIDATED, BIOLOGICALLY_VALIDATED, SIGNED, PUBLISHED
  - `samples` — `List<SampleResponse>`
  - `analyses` — `List<AnalysisRequestResponse>`
    - `id` — `String`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `requestedAt` — `Instant`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`
  - `rendezVousAt` — `Instant`

</details>

<details>
<summary><code>POST /api/v1/quotes/{id}:lock</code></summary>

- **Permission:** `quote.lock`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `QuoteResponse`
  - `id` — `String`
  - `quoteNumber` — `String`
  - `patient` — `PatientSnapshotResponse`
    - `id` — `String`
    - `labCode` — `String`
    - `lastName` — `String`
    - `firstNames` — `String`
    - `sex` — `Sex` — one of: M, F
  - `prescriber` — `PrescriberSnapshotResponse`
    - `id` — `String`
    - `code` — `String`
    - `fullName` — `String`
    - `specialtyCode` — `String`
  - `status` — `QuoteStatus` — one of: DRAFT, LOCKED, CONVERTED, EXPIRED
  - `lockedAt` — `Instant`
  - `labFileId` — `String`
  - `convertedAt` — `Instant`
  - `expiresAt` — `Instant`
  - `totalAmount` — `BigDecimal`
  - `lines` — `List<QuoteLineResponse>`
    - `id` — `String`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `indiciaryValueBSnapshot` — `BigDecimal`
    - `bRateSnapshot` — `BigDecimal`
    - `labelCountSnapshot` — `int`
    - `lineTotal` — `BigDecimal`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`

</details>

<details>
<summary><code>POST /api/v1/quotes/{id}:send</code></summary>

- **Permission:** `quote.send`
- **Success:** 202 Accepted
- **Path params:** `id` (`String`)
- **Request body:** `SendQuoteRequest`
  - `channels` — `Set<Channel>` — one of: EMAIL, WHATSAPP
- **Response body:** `QuoteSendResponse`
  - `notificationIds` — `List<NotificationId>`
    - `value` — `String`

</details>

<details>
<summary><code>GET /api/v1/samples/queues/destruction-due</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Query params:**
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `Page<SampleDestructionRow>`
  - `sampleId` — `String`
  - `dailyId` — `String`
  - `analysisCode` — `String`
  - `analysisName` — `String`
  - `portoir` — `String`
  - `conservationStart` — `Instant`
  - `destructionDueAt` — `Instant`
  - `daysOverdue` — `int`
  - `labFileId` — `String`
  - `labFileReference` — `String`

</details>

<details>
<summary><code>POST /api/v1/samples/{id}:mark-destroyed</code></summary>

- **Permission:** `sample.destroy`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)
- **Response body:** `Void`

</details>


### Results & validation

| Method | Path | Permission | Success |
|---|---|---|---|
| `GET` | `/api/v1/dossiers` | `lab-file.read` | 200 OK |
| `GET` | `/api/v1/lab-files/queues/overdue` | `lab-file.read` | 200 OK |
| `GET` | `/api/v1/lab-files/queues/urgent` | `lab-file.read` | 200 OK |
| `GET` | `/api/v1/lab-files/{id}/export` | `lab-file.read` | 200 OK |
| `DELETE` | `/api/v1/lab-files/{labFileId}/dossier-comment` | `result.enter` | 204 No Content |
| `GET` | `/api/v1/lab-files/{labFileId}/dossier-comment` | `result.enter` | 204 No Content |
| `PUT` | `/api/v1/lab-files/{labFileId}/dossier-comment` | `result.enter` | 200 OK |
| `GET` | `/api/v1/paillasses/{disciplineId}/worksheet.pdf` | `lab-file.read` | 200 OK |
| `GET` | `/api/v1/results` | `result.enter` | 200 OK |
| `GET` | `/api/v1/results/anterior` | `result.enter` | 200 OK |
| `GET` | `/api/v1/results/queues/to-print` | `result.validate-bio` | 200 OK |
| `GET` | `/api/v1/results/queues/to-validate` | `result.validate-bio` | 200 OK |
| `GET` | `/api/v1/results/search` | `result.enter` | 200 OK |
| `DELETE` | `/api/v1/results/{id}` | `result.enter` | 204 No Content |
| `GET` | `/api/v1/results/{id}` | `result.enter` | 200 OK |
| `POST` | `/api/v1/results/{id}/antibiogram` | `result.enter` | 201 Created |
| `DELETE` | `/api/v1/results/{id}/antibiogram/{readingId}` | `result.enter` | 204 No Content |
| `PATCH` | `/api/v1/results/{id}/lines/{lineId}` | `result.enter` | 200 OK |
| `DELETE` | `/api/v1/results/{id}/lines/{lineId}/value` | `result.enter` | 204 No Content |
| `POST` | `/api/v1/results/{id}:validate-biological` | `result.validate-bio` | 200 OK |
| `POST` | `/api/v1/results/{id}:validate-technical` | `result.validate-tech` | 200 OK |
| `GET` | `/api/v1/results/{labFileId}/pdf` | `lab-file.read` | 200 OK |

<details>
<summary><code>GET /api/v1/dossiers</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Query params:**
  - `state` — `DossierState`, optional — one of: VALIDATED, NOT_VALIDATED, IN_PROGRESS, DELETED
  - `urgent` — `Boolean`, optional
  - `empty` — `Boolean`, optional
  - `printed` — `Boolean`, optional
  - `signed` — `Boolean`, optional
  - `emailed` — `Boolean`, optional
  - `patientId` — `String`, optional
  - `dateFrom` — `LocalDate`, optional
  - `dateTo` — `LocalDate`, optional
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `DossierListResponse`
  - `content` — `List<DossierRow>`
    - `labFileId` — `String`
    - `registrationNumber` — `String`
    - `createdAt` — `Instant`
    - `urgent` — `boolean`
    - `patientId` — `String`
    - `patientLabCode` — `String`
    - `patientLastName` — `String`
    - `patientFirstNames` — `String`
    - `patientInsured` — `boolean`
    - `marker` — `DossierMarker` — one of: GREEN, RED, PEN
    - `printedAt` — `Instant`
    - `emailedAt` — `Instant`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>GET /api/v1/lab-files/queues/overdue</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Query params:**
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `OverdueDossierResponse`
  - `content` — `List<OverdueDossierRow>`
    - `dossier` — `DossierRow`
      - `labFileId` — `String`
      - `registrationNumber` — `String`
      - `createdAt` — `Instant`
      - `urgent` — `boolean`
      - `patientId` — `String`
      - `patientLabCode` — `String`
      - `patientLastName` — `String`
      - `patientFirstNames` — `String`
      - `patientInsured` — `boolean`
      - `marker` — `DossierMarker` — one of: GREEN, RED, PEN
      - `printedAt` — `Instant`
      - `emailedAt` — `Instant`
    - `overdue` — `OverdueInfo`
      - `analysisCode` — `String`
      - `analysisName` — `String`
      - `deadline` — `Instant`
      - `hoursPastDeadline` — `int`
      - `overdueAnalysisCount` — `int`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>GET /api/v1/lab-files/queues/urgent</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Query params:**
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `UrgentDossierResponse`
  - `content` — `List<DossierRow>`
    - `labFileId` — `String`
    - `registrationNumber` — `String`
    - `createdAt` — `Instant`
    - `urgent` — `boolean`
    - `patientId` — `String`
    - `patientLabCode` — `String`
    - `patientLastName` — `String`
    - `patientFirstNames` — `String`
    - `patientInsured` — `boolean`
    - `marker` — `DossierMarker` — one of: GREEN, RED, PEN
    - `printedAt` — `Instant`
    - `emailedAt` — `Instant`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>GET /api/v1/lab-files/{id}/export</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Produces:** varies — read the `Content-Type` response header
- **Download:** binary body with `Content-Disposition: attachment; filename="…"`
- **Path params:** `id` (`String`)
- **Query params:**
  - `format` — `String`, required
- **Response body:** `byte[]`

</details>

<details>
<summary><code>DELETE /api/v1/lab-files/{labFileId}/dossier-comment</code></summary>

- **Permission:** `result.enter`
- **Success:** 204 No Content
- **Path params:** `labFileId` (`String`)

</details>

<details>
<summary><code>GET /api/v1/lab-files/{labFileId}/dossier-comment</code></summary>

- **Permission:** `result.enter`
- **Success:** 204 No Content
- **Path params:** `labFileId` (`String`)
- **Response body:** `DossierCommentResponse`
  - `labFileId` — `String`
  - `commentText` — `String`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`

</details>

<details>
<summary><code>PUT /api/v1/lab-files/{labFileId}/dossier-comment</code></summary>

- **Permission:** `result.enter`
- **Success:** 200 OK
- **Path params:** `labFileId` (`String`)
- **Request body:** `UpsertDossierCommentRequest`
  - `commentText` — `String`
- **Response body:** `DossierCommentResponse`
  - `labFileId` — `String`
  - `commentText` — `String`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`

</details>

<details>
<summary><code>GET /api/v1/paillasses/{disciplineId}/worksheet.pdf</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Produces:** application/pdf
- **Download:** binary body with `Content-Disposition: attachment; filename="…"`
- **Path params:** `disciplineId` (`String`)
- **Query params:**
  - `from` — `LocalDate`, optional
  - `to` — `LocalDate`, optional
- **Response body:** `byte[]`

</details>

<details>
<summary><code>GET /api/v1/results</code></summary>

- **Permission:** `result.enter`
- **Success:** 200 OK
- **Query params:**
  - `labFileId` — `String`, required
- **Response body:** `List<ResultResponse>`
  - `id` — `String`
  - `labFileId` — `String`
  - `analysisRequestId` — `String`
  - `analysisId` — `String`
  - `analysisCodeSnapshot` — `String`
  - `state` — `String`
  - `technicalValidatedBy` — `String`
  - `technicalValidatedAt` — `Instant`
  - `biologicalValidatedBy` — `String`
  - `biologicalValidatedAt` — `Instant`
  - `lines` — `List<LineResponse>`
    - `id` — `String`
    - `position` — `int`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `analysisName` — `String`
    - `indiciaryValueB` — `BigDecimal`
    - `bRate` — `BigDecimal`
    - `labelCount` — `int`
    - `lineTotal` — `BigDecimal`
  - `antibiogramReadings` — `List<AntibiogramReadingResponse>`
    - `id` — `String`
    - `antibioticId` — `String`
    - `measuredDiameter` — `String`
    - `interpretedSensitivity` — `String`
    - `position` — `int`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`

</details>

<details>
<summary><code>GET /api/v1/results/anterior</code></summary>

- **Permission:** `result.enter`
- **Success:** 200 OK
- **Query params:**
  - `patientId` — `String`, required
  - `componentCode` — `String`, required
  - `limit` — `int`, optional, default `3`
- **Response body:** `List<AnteriorResultRow>`
  - `resultId` — `ResultId`
    - `value` — `String`
  - `lineId` — `ResultLineId`
    - `value` — `String`
  - `labFileId` — `LabFileId`
    - `value` — `String`
  - `labFileRegistrationNumber` — `String`
  - `componentCode` — `String`
  - `resultType` — `ResultType` — one of: FORMULA, INTEGER, REAL, TEXT, FREE
  - `valueInteger` — `Long`
  - `valueReal` — `BigDecimal`
  - `valueText` — `String`
  - `unit1Snapshot` — `String`
  - `isOutOfRange` — `boolean`
  - `biologicalValidatedAt` — `Instant`

</details>

<details>
<summary><code>GET /api/v1/results/queues/to-print</code></summary>

- **Permission:** `result.validate-bio`
- **Success:** 200 OK
- **Query params:**
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `ResultQueueResponse`
  - `content` — `List<ResultSummary>`
    - `id` — `ResultId`
      - `value` — `String`
    - `labFileId` — `LabFileId`
      - `value` — `String`
    - `analysisRequestId` — `AnalysisRequestId`
      - `value` — `String`
    - `analysisId` — `AnalysisId`
      - `value` — `String`
    - `analysisCodeSnapshot` — `String`
    - `state` — `ResultState` — one of: DRAFT, ENTERED, TECHNICAL_VALIDATED, BIOLOGICAL_VALIDATED
    - `technicalValidatedBy` — `String`
    - `technicalValidatedAt` — `Instant`
    - `biologicalValidatedBy` — `String`
    - `biologicalValidatedAt` — `Instant`
    - `lines` — `List<LineSummary>`
      - `id` — `String`
      - `position` — `int`
      - `analysisId` — `AnalysisId`
      - `analysisCodeSnapshot` — `String`
      - `analysisNameSnapshot` — `String`
      - `indiciaryValueBSnapshot` — `BigDecimal`
      - `bRateSnapshot` — `BigDecimal`
      - `labelCountSnapshot` — `int`
      - `lineTotal` — `BigDecimal`
    - `antibiogramReadings` — `List<AntibiogramReadingSummary>`
      - `id` — `AntibiogramReadingId`
      - `antibioticId` — `AntibioticId`
      - `measuredDiameter` — `BigDecimal`
      - `interpretedSensitivity` — `Sensitivity` — one of: R, I, S
      - `position` — `int`
    - `createdAt` — `Instant`
    - `createdByUserId` — `String`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>GET /api/v1/results/queues/to-validate</code></summary>

- **Permission:** `result.validate-bio`
- **Success:** 200 OK
- **Query params:**
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `ResultQueueResponse`
  - `content` — `List<ResultSummary>`
    - `id` — `ResultId`
      - `value` — `String`
    - `labFileId` — `LabFileId`
      - `value` — `String`
    - `analysisRequestId` — `AnalysisRequestId`
      - `value` — `String`
    - `analysisId` — `AnalysisId`
      - `value` — `String`
    - `analysisCodeSnapshot` — `String`
    - `state` — `ResultState` — one of: DRAFT, ENTERED, TECHNICAL_VALIDATED, BIOLOGICAL_VALIDATED
    - `technicalValidatedBy` — `String`
    - `technicalValidatedAt` — `Instant`
    - `biologicalValidatedBy` — `String`
    - `biologicalValidatedAt` — `Instant`
    - `lines` — `List<LineSummary>`
      - `id` — `String`
      - `position` — `int`
      - `analysisId` — `AnalysisId`
      - `analysisCodeSnapshot` — `String`
      - `analysisNameSnapshot` — `String`
      - `indiciaryValueBSnapshot` — `BigDecimal`
      - `bRateSnapshot` — `BigDecimal`
      - `labelCountSnapshot` — `int`
      - `lineTotal` — `BigDecimal`
    - `antibiogramReadings` — `List<AntibiogramReadingSummary>`
      - `id` — `AntibiogramReadingId`
      - `antibioticId` — `AntibioticId`
      - `measuredDiameter` — `BigDecimal`
      - `interpretedSensitivity` — `Sensitivity` — one of: R, I, S
      - `position` — `int`
    - `createdAt` — `Instant`
    - `createdByUserId` — `String`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>GET /api/v1/results/search</code></summary>

- **Permission:** `result.enter`
- **Success:** 200 OK
- **Query params:**
  - `dateFrom` — `LocalDate`, optional
  - `dateTo` — `LocalDate`, optional
  - `patientType` — `PatientType`, optional — one of: INSURED, NOT_INSURED, SOCIETY, PARTNER_LAB
  - `resultState` — `SearchResultState`, optional — one of: EMPTY, TO_CHECK, URGENT, VALIDATED, NOT_VALIDATED
  - `analysisCode` — `String`, optional
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `ResultSearchResponse`
  - `content` — `List<ResultSearchRow>`
    - `resultId` — `ResultId`
      - `value` — `String`
    - `lineId` — `ResultLineId`
      - `value` — `String`
    - `labFileId` — `LabFileId`
      - `value` — `String`
    - `labFileRegistrationNumber` — `String`
    - `patientId` — `PatientId`
      - `value` — `String`
    - `patientLabCode` — `String`
    - `patientLastName` — `String`
    - `patientFirstNames` — `String`
    - `patientInsured` — `boolean`
    - `analysisCode` — `String`
    - `componentCode` — `String`
    - `position` — `int`
    - `resultType` — `ResultType` — one of: FORMULA, INTEGER, REAL, TEXT, FREE
    - `valueInteger` — `Long`
    - `valueReal` — `BigDecimal`
    - `valueText` — `String`
    - `unit1Snapshot` — `String`
    - `isOutOfRange` — `boolean`
    - `referenceIntervalId` — `ReferenceIntervalId`
      - `value` — `String`
    - `resultState` — `ResultState` — one of: DRAFT, ENTERED, TECHNICAL_VALIDATED, BIOLOGICAL_VALIDATED
    - `biologicalValidatedAt` — `Instant`
    - `createdAt` — `Instant`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`
  - `totalPages` — `int`

</details>

<details>
<summary><code>DELETE /api/v1/results/{id}</code></summary>

- **Permission:** `result.enter`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/results/{id}</code></summary>

- **Permission:** `result.enter`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `ResultResponse`
  - `id` — `String`
  - `labFileId` — `String`
  - `analysisRequestId` — `String`
  - `analysisId` — `String`
  - `analysisCodeSnapshot` — `String`
  - `state` — `String`
  - `technicalValidatedBy` — `String`
  - `technicalValidatedAt` — `Instant`
  - `biologicalValidatedBy` — `String`
  - `biologicalValidatedAt` — `Instant`
  - `lines` — `List<LineResponse>`
    - `id` — `String`
    - `position` — `int`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `analysisName` — `String`
    - `indiciaryValueB` — `BigDecimal`
    - `bRate` — `BigDecimal`
    - `labelCount` — `int`
    - `lineTotal` — `BigDecimal`
  - `antibiogramReadings` — `List<AntibiogramReadingResponse>`
    - `id` — `String`
    - `antibioticId` — `String`
    - `measuredDiameter` — `String`
    - `interpretedSensitivity` — `String`
    - `position` — `int`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`

</details>

<details>
<summary><code>POST /api/v1/results/{id}/antibiogram</code></summary>

- **Permission:** `result.enter`
- **Success:** 201 Created
- **Path params:** `id` (`String`)
- **Request body:** `AddAntibiogramReadingRequest`
  - `antibioticId` — `String`
  - `measuredDiameter` — `String`
- **Response body:** `ResultResponse.AntibiogramReadingResponse`
  - `id` — `String`
  - `antibioticId` — `String`
  - `measuredDiameter` — `String`
  - `interpretedSensitivity` — `String`
  - `position` — `int`

</details>

<details>
<summary><code>DELETE /api/v1/results/{id}/antibiogram/{readingId}</code></summary>

- **Permission:** `result.enter`
- **Success:** 204 No Content
- **Path params:** `id` (`String`), `readingId` (`String`)

</details>

<details>
<summary><code>PATCH /api/v1/results/{id}/lines/{lineId}</code></summary>

- **Permission:** `result.enter`
- **Success:** 200 OK
- **Path params:** `id` (`String`), `lineId` (`String`)
- **Request body:** `PatchLineRequest`
  - `value` — `ValueUnion`
  - `commentText` — `String`
- **Response body:** `ResultResponse`
  - `id` — `String`
  - `labFileId` — `String`
  - `analysisRequestId` — `String`
  - `analysisId` — `String`
  - `analysisCodeSnapshot` — `String`
  - `state` — `String`
  - `technicalValidatedBy` — `String`
  - `technicalValidatedAt` — `Instant`
  - `biologicalValidatedBy` — `String`
  - `biologicalValidatedAt` — `Instant`
  - `lines` — `List<LineResponse>`
    - `id` — `String`
    - `position` — `int`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `analysisName` — `String`
    - `indiciaryValueB` — `BigDecimal`
    - `bRate` — `BigDecimal`
    - `labelCount` — `int`
    - `lineTotal` — `BigDecimal`
  - `antibiogramReadings` — `List<AntibiogramReadingResponse>`
    - `id` — `String`
    - `antibioticId` — `String`
    - `measuredDiameter` — `String`
    - `interpretedSensitivity` — `String`
    - `position` — `int`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`

</details>

<details>
<summary><code>DELETE /api/v1/results/{id}/lines/{lineId}/value</code></summary>

- **Permission:** `result.enter`
- **Success:** 204 No Content
- **Path params:** `id` (`String`), `lineId` (`String`)

</details>

<details>
<summary><code>POST /api/v1/results/{id}:validate-biological</code></summary>

- **Permission:** `result.validate-bio`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `ResultResponse`
  - `id` — `String`
  - `labFileId` — `String`
  - `analysisRequestId` — `String`
  - `analysisId` — `String`
  - `analysisCodeSnapshot` — `String`
  - `state` — `String`
  - `technicalValidatedBy` — `String`
  - `technicalValidatedAt` — `Instant`
  - `biologicalValidatedBy` — `String`
  - `biologicalValidatedAt` — `Instant`
  - `lines` — `List<LineResponse>`
    - `id` — `String`
    - `position` — `int`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `analysisName` — `String`
    - `indiciaryValueB` — `BigDecimal`
    - `bRate` — `BigDecimal`
    - `labelCount` — `int`
    - `lineTotal` — `BigDecimal`
  - `antibiogramReadings` — `List<AntibiogramReadingResponse>`
    - `id` — `String`
    - `antibioticId` — `String`
    - `measuredDiameter` — `String`
    - `interpretedSensitivity` — `String`
    - `position` — `int`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`

</details>

<details>
<summary><code>POST /api/v1/results/{id}:validate-technical</code></summary>

- **Permission:** `result.validate-tech`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `ResultResponse`
  - `id` — `String`
  - `labFileId` — `String`
  - `analysisRequestId` — `String`
  - `analysisId` — `String`
  - `analysisCodeSnapshot` — `String`
  - `state` — `String`
  - `technicalValidatedBy` — `String`
  - `technicalValidatedAt` — `Instant`
  - `biologicalValidatedBy` — `String`
  - `biologicalValidatedAt` — `Instant`
  - `lines` — `List<LineResponse>`
    - `id` — `String`
    - `position` — `int`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `analysisName` — `String`
    - `indiciaryValueB` — `BigDecimal`
    - `bRate` — `BigDecimal`
    - `labelCount` — `int`
    - `lineTotal` — `BigDecimal`
  - `antibiogramReadings` — `List<AntibiogramReadingResponse>`
    - `id` — `String`
    - `antibioticId` — `String`
    - `measuredDiameter` — `String`
    - `interpretedSensitivity` — `String`
    - `position` — `int`
  - `createdAt` — `Instant`
  - `createdByUserId` — `String`

</details>

<details>
<summary><code>GET /api/v1/results/{labFileId}/pdf</code></summary>

- **Permission:** `lab-file.read`
- **Success:** 200 OK
- **Produces:** application/pdf
- **Download:** binary body with `Content-Disposition: attachment; filename="…"`
- **Path params:** `labFileId` (`String`)
- **Response body:** `byte[]`

</details>


### Delivery, signature & patient portal

| Method | Path | Permission | Success |
|---|---|---|---|
| `POST` | `/api/v1/lab-files/{id}:mark-printed` | `dossier.print` | 204 No Content |
| `POST` | `/api/v1/lab-files/{id}:publish` | `dossier.publish` | 200 OK |
| `POST` | `/api/v1/lab-files/{id}:send` | `dossier.send` | 202 Accepted |
| `POST` | `/api/v1/lab-files/{id}:sign` | `dossier.sign-electronically` | 200 OK |
| `GET` | `/api/v1/portal/dossiers` | — | 200 OK |
| `GET` | `/api/v1/portal/dossiers/{labFileId}/pdf` | — | 200 OK |
| `POST` | `/api/v1/portal/login` | — | 200 OK |

<details>
<summary><code>POST /api/v1/lab-files/{id}:mark-printed</code></summary>

- **Permission:** `dossier.print`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)
- **Response body:** `Void`

</details>

<details>
<summary><code>POST /api/v1/lab-files/{id}:publish</code></summary>

- **Permission:** `dossier.publish`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `Void`

</details>

<details>
<summary><code>POST /api/v1/lab-files/{id}:send</code></summary>

- **Permission:** `dossier.send`
- **Success:** 202 Accepted
- **Path params:** `id` (`String`)
- **Response body:** `SendDossierResponse`
  - `emailNotificationId` — `NotificationId`
    - `value` — `String`
  - `whatsAppNotificationId` — `NotificationId`
    - `value` — `String`
  - `password` — `String`

</details>

<details>
<summary><code>POST /api/v1/lab-files/{id}:sign</code></summary>

- **Permission:** `dossier.sign-electronically`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `Void`

</details>

<details>
<summary><code>GET /api/v1/portal/dossiers</code></summary>

- **Permission:** authenticated only
- **Success:** 200 OK
- **Response body:** `List<PortalDossierResponse>`
  - `id` — `String`
  - `registrationNumber` — `String`
  - `createdAt` — `Instant`
  - `analysesCount` — `int`

</details>

<details>
<summary><code>GET /api/v1/portal/dossiers/{labFileId}/pdf</code></summary>

- **Permission:** authenticated only
- **Success:** 200 OK
- **Produces:** application/pdf
- **Download:** binary body with `Content-Disposition: attachment; filename="…"`
- **Path params:** `labFileId` (`String`)
- **Response body:** `byte[]`

</details>

<details>
<summary><code>POST /api/v1/portal/login</code></summary>

- **Permission:** authenticated only
- **Success:** 200 OK
- **Request body:** `PortalLoginRequest`
  - `portalLogin` — `String`
  - `password` — `String`
- **Response body:** `PortalLoginResponse`
  - `token` — `String`
  - `expiresInSeconds` — `long`

</details>


### Billing, invoices & accounting

| Method | Path | Permission | Success |
|---|---|---|---|
| `GET` | `/api/v1/accounting/by-payment-mode` | `accounting.read` | 200 OK |
| `GET` | `/api/v1/accounting/daily` | `accounting.read` | 200 OK |
| `GET` | `/api/v1/accounting/outstanding` | `accounting.read` | 200 OK |
| `GET` | `/api/v1/cash-registers` | `cash-register.manage` | 200 OK |
| `POST` | `/api/v1/cash-registers` | `cash-register.manage` | 201 Created |
| `DELETE` | `/api/v1/cash-registers/{id}` | `cash-register.manage` | 204 No Content |
| `GET` | `/api/v1/cash-registers/{id}` | `cash-register.manage` | 200 OK |
| `PUT` | `/api/v1/cash-registers/{id}` | `cash-register.manage` | 200 OK |
| `PUT` | `/api/v1/cash-registers/{id}/users` | `cash-register.manage` | 200 OK |
| `GET` | `/api/v1/invoices` | `invoice.read` | 200 OK |
| `POST` | `/api/v1/invoices` | `invoice.create` | 201 Created |
| `GET` | `/api/v1/invoices/{id}` | `invoice.read` | 200 OK |
| `GET` | `/api/v1/lab-files/{labFileId}/payments` | `invoice.read` | 200 OK |
| `POST` | `/api/v1/payments` | `payment.collect` | 201 Created |
| `GET` | `/api/v1/payments/{id}/receipt.pdf` | `invoice.read` | 200 OK |

<details>
<summary><code>GET /api/v1/accounting/by-payment-mode</code></summary>

- **Permission:** `accounting.read`
- **Success:** 200 OK
- **Query params:**
  - `from` — `Instant`, required
  - `to` — `Instant`, required
- **Response body:** `AccountingByModeResponse`
  - `rows` — `List<ModeRow>`
    - `mode` — `String`
    - `totalAmount` — `BigDecimal`
    - `paymentCount` — `long`
  - `totals` — `AccountingDailyResponse.Totals`
    - `totalAmount` — `BigDecimal`
    - `paidAmount` — `BigDecimal`
    - `remainingAmount` — `BigDecimal`

</details>

<details>
<summary><code>GET /api/v1/accounting/daily</code></summary>

- **Permission:** `accounting.read`
- **Success:** 200 OK
- **Query params:**
  - `date` — `LocalDate`, required
- **Response body:** `AccountingDailyResponse`
  - `files` — `List<FileRow>`
    - `labFileId` — `String`
    - `registrationNumber` — `String`
    - `customerName` — `String`
    - `totalAmount` — `BigDecimal`
    - `paidAmount` — `BigDecimal`
    - `remainingAmount` — `BigDecimal`
  - `totals` — `Totals`
    - `totalAmount` — `BigDecimal`
    - `paidAmount` — `BigDecimal`
    - `remainingAmount` — `BigDecimal`

</details>

<details>
<summary><code>GET /api/v1/accounting/outstanding</code></summary>

- **Permission:** `accounting.read`
- **Success:** 200 OK
- **Response body:** `AccountingOutstandingResponse`
  - `unpaid` — `List<AccountingDailyResponse.FileRow>`
    - `labFileId` — `String`
    - `registrationNumber` — `String`
    - `customerName` — `String`
    - `totalAmount` — `BigDecimal`
    - `paidAmount` — `BigDecimal`
    - `remainingAmount` — `BigDecimal`
  - `refundsOwed` — `List<AccountingDailyResponse.FileRow>`
    - `labFileId` — `String`
    - `registrationNumber` — `String`
    - `customerName` — `String`
    - `totalAmount` — `BigDecimal`
    - `paidAmount` — `BigDecimal`
    - `remainingAmount` — `BigDecimal`
  - `unpaidTotals` — `AccountingDailyResponse.Totals`
    - `totalAmount` — `BigDecimal`
    - `paidAmount` — `BigDecimal`
    - `remainingAmount` — `BigDecimal`
  - `refundsOwedTotals` — `AccountingDailyResponse.Totals`
    - `totalAmount` — `BigDecimal`
    - `paidAmount` — `BigDecimal`
    - `remainingAmount` — `BigDecimal`

</details>

<details>
<summary><code>GET /api/v1/cash-registers</code></summary>

- **Permission:** `cash-register.manage`
- **Success:** 200 OK
- **Query params:**
  - `q` — `String`, optional
  - `disabled` — `Boolean`, optional
- **Response body:** `CashRegisterSearchResponse`
  - `items` — `List<CashRegisterResponse>`
    - `id` — `String`
    - `code` — `String`
    - `label` — `String`
    - `sequenceNumber` — `Integer`
    - `userIds` — `Set<String>`
    - `disabled` — `boolean`
    - `createdAt` — `Instant`
  - `totalElements` — `long`

</details>

<details>
<summary><code>POST /api/v1/cash-registers</code></summary>

- **Permission:** `cash-register.manage`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `CashRegisterRequest.Create`
- **Response body:** `CashRegisterResponse`
  - `id` — `String`
  - `code` — `String`
  - `label` — `String`
  - `sequenceNumber` — `Integer`
  - `userIds` — `Set<String>`
  - `disabled` — `boolean`
  - `createdAt` — `Instant`

</details>

<details>
<summary><code>DELETE /api/v1/cash-registers/{id}</code></summary>

- **Permission:** `cash-register.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>GET /api/v1/cash-registers/{id}</code></summary>

- **Permission:** `cash-register.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `CashRegisterResponse`
  - `id` — `String`
  - `code` — `String`
  - `label` — `String`
  - `sequenceNumber` — `Integer`
  - `userIds` — `Set<String>`
  - `disabled` — `boolean`
  - `createdAt` — `Instant`

</details>

<details>
<summary><code>PUT /api/v1/cash-registers/{id}</code></summary>

- **Permission:** `cash-register.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `CashRegisterRequest.Update`
- **Response body:** `CashRegisterResponse`
  - `id` — `String`
  - `code` — `String`
  - `label` — `String`
  - `sequenceNumber` — `Integer`
  - `userIds` — `Set<String>`
  - `disabled` — `boolean`
  - `createdAt` — `Instant`

</details>

<details>
<summary><code>PUT /api/v1/cash-registers/{id}/users</code></summary>

- **Permission:** `cash-register.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `CashRegisterUserAssignmentRequest`
  - `userIds` — `Set<String>`
- **Response body:** `CashRegisterResponse`
  - `id` — `String`
  - `code` — `String`
  - `label` — `String`
  - `sequenceNumber` — `Integer`
  - `userIds` — `Set<String>`
  - `disabled` — `boolean`
  - `createdAt` — `Instant`

</details>

<details>
<summary><code>GET /api/v1/invoices</code></summary>

- **Permission:** `invoice.read`
- **Success:** 200 OK
- **Query params:**
  - `from` — `Instant`, optional
  - `to` — `Instant`, optional
  - `customerKind` — `InvoiceCustomerKind`, optional — one of: PATIENT, INSURANCE, PARTNER
  - `customerId` — `String`, optional
  - `labFileId` — `String`, optional
  - `patientId` — `String`, optional
  - `page` — `int`, optional, default `0`
  - `size` — `int`, optional, default `20`
- **Response body:** `InvoiceSearchResponse`
  - `items` — `List<InvoiceResponse>`
    - `id` — `String`
    - `number` — `String`
    - `labFileId` — `String`
    - `issuedAt` — `Instant`
    - `customerKind` — `InvoiceCustomerKind` — one of: PATIENT, INSURANCE, PARTNER
    - `customerId` — `String`
    - `customerName` — `String`
    - `homeCollectionFee` — `BigDecimal`
    - `additionalFees` — `BigDecimal`
    - `discountKind` — `DiscountKind` — one of: AMOUNT, PERCENT
    - `discountValue` — `BigDecimal`
    - `totalAmount` — `BigDecimal`
    - `insuranceCoverageAmount` — `BigDecimal`
    - `discountAmount` — `BigDecimal`
    - `netAmount` — `BigDecimal`
    - `lines` — `List<LineResponse>`
      - `id` — `String`
      - `position` — `int`
      - `analysisId` — `String`
      - `analysisCode` — `String`
      - `analysisName` — `String`
      - `indiciaryValueB` — `BigDecimal`
      - `bRate` — `BigDecimal`
      - `labelCount` — `int`
      - `lineTotal` — `BigDecimal`
  - `total` — `long`

</details>

<details>
<summary><code>POST /api/v1/invoices</code></summary>

- **Permission:** `invoice.create`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `InvoiceRequest`
  - `labFileId` — `String`
- **Response body:** `InvoiceResponse`
  - `id` — `String`
  - `number` — `String`
  - `labFileId` — `String`
  - `issuedAt` — `Instant`
  - `customerKind` — `InvoiceCustomerKind` — one of: PATIENT, INSURANCE, PARTNER
  - `customerId` — `String`
  - `customerName` — `String`
  - `homeCollectionFee` — `BigDecimal`
  - `additionalFees` — `BigDecimal`
  - `discountKind` — `DiscountKind` — one of: AMOUNT, PERCENT
  - `discountValue` — `BigDecimal`
  - `totalAmount` — `BigDecimal`
  - `insuranceCoverageAmount` — `BigDecimal`
  - `discountAmount` — `BigDecimal`
  - `netAmount` — `BigDecimal`
  - `lines` — `List<LineResponse>`
    - `id` — `String`
    - `position` — `int`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `analysisName` — `String`
    - `indiciaryValueB` — `BigDecimal`
    - `bRate` — `BigDecimal`
    - `labelCount` — `int`
    - `lineTotal` — `BigDecimal`

</details>

<details>
<summary><code>GET /api/v1/invoices/{id}</code></summary>

- **Permission:** `invoice.read`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Response body:** `InvoiceResponse`
  - `id` — `String`
  - `number` — `String`
  - `labFileId` — `String`
  - `issuedAt` — `Instant`
  - `customerKind` — `InvoiceCustomerKind` — one of: PATIENT, INSURANCE, PARTNER
  - `customerId` — `String`
  - `customerName` — `String`
  - `homeCollectionFee` — `BigDecimal`
  - `additionalFees` — `BigDecimal`
  - `discountKind` — `DiscountKind` — one of: AMOUNT, PERCENT
  - `discountValue` — `BigDecimal`
  - `totalAmount` — `BigDecimal`
  - `insuranceCoverageAmount` — `BigDecimal`
  - `discountAmount` — `BigDecimal`
  - `netAmount` — `BigDecimal`
  - `lines` — `List<LineResponse>`
    - `id` — `String`
    - `position` — `int`
    - `analysisId` — `String`
    - `analysisCode` — `String`
    - `analysisName` — `String`
    - `indiciaryValueB` — `BigDecimal`
    - `bRate` — `BigDecimal`
    - `labelCount` — `int`
    - `lineTotal` — `BigDecimal`

</details>

<details>
<summary><code>GET /api/v1/lab-files/{labFileId}/payments</code></summary>

- **Permission:** `invoice.read`
- **Success:** 200 OK
- **Path params:** `labFileId` (`String`)
- **Response body:** `List<PaymentResponse>`
  - `id` — `String`
  - `labFileId` — `String`
  - `invoiceId` — `String`
  - `mode` — `PaymentMode` — one of: CASH, CHEQUE, CARD_TPE, FLOOZ, MIXX_BY_YAS
  - `amount` — `BigDecimal`
  - `cashRegisterId` — `String`
  - `recordedByUserId` — `String`
  - `recordedAt` — `Instant`
  - `note` — `String`

</details>

<details>
<summary><code>POST /api/v1/payments</code></summary>

- **Permission:** `payment.collect`
- **Success:** 201 Created
- **`Location`** response header points at the created resource
- **Request body:** `PaymentRequest`
  - `labFileId` — `String`
  - `mode` — `PaymentMode` — one of: CASH, CHEQUE, CARD_TPE, FLOOZ, MIXX_BY_YAS
  - `amount` — `BigDecimal`
  - `cashRegisterId` — `String`
  - `note` — `String`
- **Response body:** `PaymentResponse`
  - `id` — `String`
  - `labFileId` — `String`
  - `invoiceId` — `String`
  - `mode` — `PaymentMode` — one of: CASH, CHEQUE, CARD_TPE, FLOOZ, MIXX_BY_YAS
  - `amount` — `BigDecimal`
  - `cashRegisterId` — `String`
  - `recordedByUserId` — `String`
  - `recordedAt` — `Instant`
  - `note` — `String`

</details>

<details>
<summary><code>GET /api/v1/payments/{id}/receipt.pdf</code></summary>

- **Permission:** `invoice.read`
- **Success:** 200 OK
- **Produces:** application/pdf
- **Download:** binary body with `Content-Disposition: attachment; filename="…"`
- **Path params:** `id` (`String`)
- **Response body:** `byte[]`

</details>


### Cashbox, expenses & journal

| Method | Path | Permission | Success |
|---|---|---|---|
| `GET` | `/api/v1/cashbox/expense-categories` | `cashbox.expense-category.read` | 200 OK |
| `POST` | `/api/v1/cashbox/expense-categories` | `cashbox.expense-category.manage` | 201 Created |
| `DELETE` | `/api/v1/cashbox/expense-categories/{id}` | `cashbox.expense-category.manage` | 204 No Content |
| `PATCH` | `/api/v1/cashbox/expense-categories/{id}` | `cashbox.expense-category.manage` | 200 OK |
| `GET` | `/api/v1/cashbox/expenses` | `cashbox.expense.read` | 200 OK |
| `POST` | `/api/v1/cashbox/expenses` | `cashbox.expense.record` | 201 Created |
| `GET` | `/api/v1/cashbox/expenses/{expenseId}/attachments` | `cashbox.expense.read` | 200 OK |
| `POST` | `/api/v1/cashbox/expenses/{expenseId}/attachments` | `cashbox.expense.record` | 201 Created |
| `DELETE` | `/api/v1/cashbox/expenses/{expenseId}/attachments/{attachmentId}` | `cashbox.expense.record` | 204 No Content |
| `GET` | `/api/v1/cashbox/expenses/{expenseId}/attachments/{attachmentId}` | `cashbox.expense.read` | 200 OK |
| `GET` | `/api/v1/cashbox/home-collection-fees` | `cashbox.home-collection.read` | 200 OK |
| `POST` | `/api/v1/cashbox/home-collection-fees` | `cashbox.home-collection.record` | 201 Created |
| `GET` | `/api/v1/cashbox/home-collection-fees/{feeId}/attachments` | `cashbox.home-collection.read` | 200 OK |
| `POST` | `/api/v1/cashbox/home-collection-fees/{feeId}/attachments` | `cashbox.home-collection.record` | 201 Created |
| `DELETE` | `/api/v1/cashbox/home-collection-fees/{feeId}/attachments/{attachmentId}` | `cashbox.home-collection.record` | 204 No Content |
| `GET` | `/api/v1/cashbox/home-collection-fees/{feeId}/attachments/{attachmentId}` | `cashbox.home-collection.read` | 200 OK |
| `GET` | `/api/v1/cashbox/journal` | `cashbox.journal.read` | 200 OK |
| `GET` | `/api/v1/cashbox/sessions/{cashRegisterId}/{sessionDate}` | `cashbox.journal.read` | 200 OK |
| `POST` | `/api/v1/cashbox/sessions/{cashRegisterId}/{sessionDate}:close` | `cashbox.session.close` | 200 OK |
| `POST` | `/api/v1/cashbox/sessions/{cashRegisterId}/{sessionDate}:reopen` | `cashbox.session.reopen` | 200 OK |

<details>
<summary><code>GET /api/v1/cashbox/expense-categories</code></summary>

- **Permission:** `cashbox.expense-category.read`
- **Success:** 200 OK
- **Query params:**
  - `active` — `Boolean`, optional
- **Response body:** `List<ExpenseCategoryResponse>`
  - `id` — `String`
  - `code` — `String`
  - `label` — `String`
  - `active` — `boolean`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`

</details>

<details>
<summary><code>POST /api/v1/cashbox/expense-categories</code></summary>

- **Permission:** `cashbox.expense-category.manage`
- **Success:** 201 Created
- **Request body:** `ExpenseCategoryRequest`
  - `code` — `String`
  - `label` — `String`
  - `active` — `Boolean`
- **Response body:** `ExpenseCategoryResponse`
  - `id` — `String`
  - `code` — `String`
  - `label` — `String`
  - `active` — `boolean`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`

</details>

<details>
<summary><code>DELETE /api/v1/cashbox/expense-categories/{id}</code></summary>

- **Permission:** `cashbox.expense-category.manage`
- **Success:** 204 No Content
- **Path params:** `id` (`String`)

</details>

<details>
<summary><code>PATCH /api/v1/cashbox/expense-categories/{id}</code></summary>

- **Permission:** `cashbox.expense-category.manage`
- **Success:** 200 OK
- **Path params:** `id` (`String`)
- **Request body:** `ExpenseCategoryRequest`
  - `code` — `String`
  - `label` — `String`
  - `active` — `Boolean`
- **Response body:** `ExpenseCategoryResponse`
  - `id` — `String`
  - `code` — `String`
  - `label` — `String`
  - `active` — `boolean`
  - `createdAt` — `Instant`
  - `updatedAt` — `Instant`

</details>

<details>
<summary><code>GET /api/v1/cashbox/expenses</code></summary>

- **Permission:** `cashbox.expense.read`
- **Success:** 200 OK
- **Query params:**
  - `from` — `Instant`, optional
  - `to` — `Instant`, optional
  - `categoryId` — `String`, optional
  - `recordedBy` — `String`, optional
  - `cashRegisterId` — `String`, optional
- **Response body:** `ExpenseSearchResponse`
  - `items` — `List<ExpenseResponse>`
    - `id` — `String`
    - `categoryId` — `String`
    - `mode` — `PaymentMode` — one of: CASH, CHEQUE, CARD_TPE, FLOOZ, MIXX_BY_YAS
    - `amount` — `BigDecimal`
    - `description` — `String`
    - `justificationScanUri` — `String`
    - `cashRegisterId` — `String`
    - `recordedBy` — `String`
    - `recordedAt` — `Instant`
  - `totalElements` — `long`

</details>

<details>
<summary><code>POST /api/v1/cashbox/expenses</code></summary>

- **Permission:** `cashbox.expense.record`
- **Success:** 201 Created
- **Request body:** `ExpenseRequest`
  - `categoryId` — `String`
  - `mode` — `PaymentMode` — one of: CASH, CHEQUE, CARD_TPE, FLOOZ, MIXX_BY_YAS
  - `amount` — `BigDecimal`
  - `description` — `String`
  - `justificationScanUri` — `String`
  - `cashRegisterId` — `String`
- **Response body:** `ExpenseResponse`
  - `id` — `String`
  - `categoryId` — `String`
  - `mode` — `PaymentMode` — one of: CASH, CHEQUE, CARD_TPE, FLOOZ, MIXX_BY_YAS
  - `amount` — `BigDecimal`
  - `description` — `String`
  - `justificationScanUri` — `String`
  - `cashRegisterId` — `String`
  - `recordedBy` — `String`
  - `recordedAt` — `Instant`

</details>

<details>
<summary><code>GET /api/v1/cashbox/expenses/{expenseId}/attachments</code></summary>

- **Permission:** `cashbox.expense.read`
- **Success:** 200 OK
- **Path params:** `expenseId` (`String`)
- **Response body:** `List<AttachmentSummary>`
  - `id` — `String`
  - `type` — `AttachableType` — one of: LAB_FILE, EXPENSE, HOME_COLLECTION_FEE
  - `targetId` — `String`
  - `filename` — `String`
  - `contentType` — `String`
  - `sizeBytes` — `long`
  - `sha256` — `String`
  - `storageUri` — `String`
  - `uploadedByUserId` — `String`
  - `uploadedAt` — `Instant`

</details>

<details>
<summary><code>POST /api/v1/cashbox/expenses/{expenseId}/attachments</code></summary>

- **Permission:** `cashbox.expense.record`
- **Success:** 201 Created
- **Consumes:** `multipart/form-data`
- **Path params:** `expenseId` (`String`)
- **Multipart part `file`:** `MultipartFile`
- **Response body:** `AttachmentSummary`
  - `id` — `String`
  - `type` — `AttachableType` — one of: LAB_FILE, EXPENSE, HOME_COLLECTION_FEE
  - `targetId` — `String`
  - `filename` — `String`
  - `contentType` — `String`
  - `sizeBytes` — `long`
  - `sha256` — `String`
  - `storageUri` — `String`
  - `uploadedByUserId` — `String`
  - `uploadedAt` — `Instant`

</details>

<details>
<summary><code>DELETE /api/v1/cashbox/expenses/{expenseId}/attachments/{attachmentId}</code></summary>

- **Permission:** `cashbox.expense.record`
- **Success:** 204 No Content
- **Path params:** `expenseId` (`String`), `attachmentId` (`String`)

</details>

<details>
<summary><code>GET /api/v1/cashbox/expenses/{expenseId}/attachments/{attachmentId}</code></summary>

- **Permission:** `cashbox.expense.read`
- **Success:** 200 OK
- **Produces:** varies — read the `Content-Type` response header
- **Download:** binary body with `Content-Disposition: attachment; filename="…"`
- **Path params:** `expenseId` (`String`), `attachmentId` (`String`)
- **Response body:** `byte[]`

</details>

<details>
<summary><code>GET /api/v1/cashbox/home-collection-fees</code></summary>

- **Permission:** `cashbox.home-collection.read`
- **Success:** 200 OK
- **Query params:**
  - `from` — `Instant`, optional
  - `to` — `Instant`, optional
  - `labFileId` — `String`, optional
  - `recordedBy` — `String`, optional
  - `cashRegisterId` — `String`, optional
- **Response body:** `HomeCollectionFeeSearchResponse`
  - `items` — `List<HomeCollectionFeeResponse>`
    - `id` — `String`
    - `patientId` — `String`
    - `labFileId` — `String`
    - `distanceKm` — `BigDecimal`
    - `amount` — `BigDecimal`
    - `receiptScanUri` — `String`
    - `cashRegisterId` — `String`
    - `recordedBy` — `String`
    - `recordedAt` — `Instant`
  - `totalElements` — `long`

</details>

<details>
<summary><code>POST /api/v1/cashbox/home-collection-fees</code></summary>

- **Permission:** `cashbox.home-collection.record`
- **Success:** 201 Created
- **Request body:** `HomeCollectionFeeRequest`
  - `patientId` — `String`
  - `labFileId` — `String`
  - `distanceKm` — `BigDecimal`
  - `amount` — `BigDecimal`
  - `receiptScanUri` — `String`
  - `cashRegisterId` — `String`
- **Response body:** `HomeCollectionFeeResponse`
  - `id` — `String`
  - `patientId` — `String`
  - `labFileId` — `String`
  - `distanceKm` — `BigDecimal`
  - `amount` — `BigDecimal`
  - `receiptScanUri` — `String`
  - `cashRegisterId` — `String`
  - `recordedBy` — `String`
  - `recordedAt` — `Instant`

</details>

<details>
<summary><code>GET /api/v1/cashbox/home-collection-fees/{feeId}/attachments</code></summary>

- **Permission:** `cashbox.home-collection.read`
- **Success:** 200 OK
- **Path params:** `feeId` (`String`)
- **Response body:** `List<AttachmentSummary>`
  - `id` — `String`
  - `type` — `AttachableType` — one of: LAB_FILE, EXPENSE, HOME_COLLECTION_FEE
  - `targetId` — `String`
  - `filename` — `String`
  - `contentType` — `String`
  - `sizeBytes` — `long`
  - `sha256` — `String`
  - `storageUri` — `String`
  - `uploadedByUserId` — `String`
  - `uploadedAt` — `Instant`

</details>

<details>
<summary><code>POST /api/v1/cashbox/home-collection-fees/{feeId}/attachments</code></summary>

- **Permission:** `cashbox.home-collection.record`
- **Success:** 201 Created
- **Consumes:** `multipart/form-data`
- **Path params:** `feeId` (`String`)
- **Multipart part `file`:** `MultipartFile`
- **Response body:** `AttachmentSummary`
  - `id` — `String`
  - `type` — `AttachableType` — one of: LAB_FILE, EXPENSE, HOME_COLLECTION_FEE
  - `targetId` — `String`
  - `filename` — `String`
  - `contentType` — `String`
  - `sizeBytes` — `long`
  - `sha256` — `String`
  - `storageUri` — `String`
  - `uploadedByUserId` — `String`
  - `uploadedAt` — `Instant`

</details>

<details>
<summary><code>DELETE /api/v1/cashbox/home-collection-fees/{feeId}/attachments/{attachmentId}</code></summary>

- **Permission:** `cashbox.home-collection.record`
- **Success:** 204 No Content
- **Path params:** `feeId` (`String`), `attachmentId` (`String`)

</details>

<details>
<summary><code>GET /api/v1/cashbox/home-collection-fees/{feeId}/attachments/{attachmentId}</code></summary>

- **Permission:** `cashbox.home-collection.read`
- **Success:** 200 OK
- **Produces:** varies — read the `Content-Type` response header
- **Download:** binary body with `Content-Disposition: attachment; filename="…"`
- **Path params:** `feeId` (`String`), `attachmentId` (`String`)
- **Response body:** `byte[]`

</details>

<details>
<summary><code>GET /api/v1/cashbox/journal</code></summary>

- **Permission:** `cashbox.journal.read`
- **Success:** 200 OK
- **Query params:**
  - `from` — `Instant`, required
  - `to` — `Instant`, required
  - `cashRegisterId` — `String`, optional
  - `operationKind` — `OperationKind`, optional — one of: PAYMENT, EXPENSE, HOME_COLLECTION_FEE
- **Response body:** `JournalResponse`
  - `items` — `List<JournalEntryRow>`
    - `occurredAt` — `Instant`
    - `operationKind` — `OperationKind` — one of: PAYMENT, EXPENSE, HOME_COLLECTION_FEE
    - `userId` — `String`
    - `userLabel` — `String`
    - `labFileId` — `String`
    - `labFileNumber` — `String`
    - `patientId` — `String`
    - `patientLabel` — `String`
    - `paymentMode` — `PaymentMode` — one of: CASH, CHEQUE, CARD_TPE, FLOOZ, MIXX_BY_YAS
    - `amount` — `BigDecimal`
    - `invoiceTotal` — `BigDecimal`
    - `paidSoFar` — `BigDecimal`
    - `remaining` — `BigDecimal`
    - `cashRegisterId` — `String`
  - `page` — `int`
  - `size` — `int`
  - `totalElements` — `long`

</details>

<details>
<summary><code>GET /api/v1/cashbox/sessions/{cashRegisterId}/{sessionDate}</code></summary>

- **Permission:** `cashbox.journal.read`
- **Success:** 200 OK
- **Path params:** `cashRegisterId` (`String`), `sessionDate` (`LocalDate`)
- **Response body:** `CashSessionResponse`
  - `cashRegisterId` — `String`
  - `sessionDate` — `LocalDate`
  - `status` — `SessionStatus` — one of: OPEN, CLOSED
  - `cashTotal` — `BigDecimal`
  - `chequeTotal` — `BigDecimal`
  - `cardTpeTotal` — `BigDecimal`
  - `floozTotal` — `BigDecimal`
  - `mixxByYasTotal` — `BigDecimal`
  - `expensesTotal` — `BigDecimal`
  - `homeCollectionTotal` — `BigDecimal`
  - `grandTotal` — `BigDecimal`
  - `closedBy` — `String`
  - `closedAt` — `Instant`
  - `reopenedBy` — `String`
  - `reopenedAt` — `Instant`
  - `reopenReason` — `String`

</details>

<details>
<summary><code>POST /api/v1/cashbox/sessions/{cashRegisterId}/{sessionDate}:close</code></summary>

- **Permission:** `cashbox.session.close`
- **Success:** 200 OK
- **Path params:** `cashRegisterId` (`String`), `sessionDate` (`LocalDate`)
- **Response body:** `CashSessionResponse`
  - `cashRegisterId` — `String`
  - `sessionDate` — `LocalDate`
  - `status` — `SessionStatus` — one of: OPEN, CLOSED
  - `cashTotal` — `BigDecimal`
  - `chequeTotal` — `BigDecimal`
  - `cardTpeTotal` — `BigDecimal`
  - `floozTotal` — `BigDecimal`
  - `mixxByYasTotal` — `BigDecimal`
  - `expensesTotal` — `BigDecimal`
  - `homeCollectionTotal` — `BigDecimal`
  - `grandTotal` — `BigDecimal`
  - `closedBy` — `String`
  - `closedAt` — `Instant`
  - `reopenedBy` — `String`
  - `reopenedAt` — `Instant`
  - `reopenReason` — `String`

</details>

<details>
<summary><code>POST /api/v1/cashbox/sessions/{cashRegisterId}/{sessionDate}:reopen</code></summary>

- **Permission:** `cashbox.session.reopen`
- **Success:** 200 OK
- **Path params:** `cashRegisterId` (`String`), `sessionDate` (`LocalDate`)
- **Request body:** `ReopenRequest`
  - `reason` — `String`
- **Response body:** `CashSessionResponse`
  - `cashRegisterId` — `String`
  - `sessionDate` — `LocalDate`
  - `status` — `SessionStatus` — one of: OPEN, CLOSED
  - `cashTotal` — `BigDecimal`
  - `chequeTotal` — `BigDecimal`
  - `cardTpeTotal` — `BigDecimal`
  - `floozTotal` — `BigDecimal`
  - `mixxByYasTotal` — `BigDecimal`
  - `expensesTotal` — `BigDecimal`
  - `homeCollectionTotal` — `BigDecimal`
  - `grandTotal` — `BigDecimal`
  - `closedBy` — `String`
  - `closedAt` — `Instant`
  - `reopenedBy` — `String`
  - `reopenedAt` — `Instant`
  - `reopenReason` — `String`

</details>


### Statistics

| Method | Path | Permission | Success |
|---|---|---|---|
| `GET` | `/api/v1/stats/activity` | `stats.read` | 200 OK |
| `GET` | `/api/v1/stats/analyses` | `stats.read` | 200 OK |
| `GET` | `/api/v1/stats/antibiograms` | `stats.read` | 200 OK |
| `GET` | `/api/v1/stats/by-discipline` | `stats.read` | 200 OK |
| `GET` | `/api/v1/stats/by-prescriber` | `stats.read` | 200 OK |
| `GET` | `/api/v1/stats/export` | `stats.read` | 200 OK |
| `GET` | `/api/v1/stats/patients` | `stats.read` | 200 OK |

<details>
<summary><code>GET /api/v1/stats/activity</code></summary>

- **Permission:** `stats.read`
- **Success:** 200 OK
- **Query params:**
  - `from` — `LocalDate`, required
  - `to` — `LocalDate`, required
  - `insuranceId` — `String`, optional
  - `partnerId` — `String`, optional
  - `prescriberId` — `String`, optional
  - `sex` — `String`, optional
- **Response body:** `ActivityResponse`
  - `dossiers` — `long`
  - `urgents` — `long`
  - `validated` — `long`
  - `patientsAll` — `long`
  - `patientsNew` — `long`
  - `patientsReturning` — `long`

</details>

<details>
<summary><code>GET /api/v1/stats/analyses</code></summary>

- **Permission:** `stats.read`
- **Success:** 200 OK
- **Query params:**
  - `from` — `LocalDate`, required
  - `to` — `LocalDate`, required
  - `disciplineId` — `String`, optional
- **Response body:** `AnalysesResponse`
  - `rows` — `List<Row>`
    - `dailyId` — `String`
    - `code` — `String`
    - `analysisName` — `String`
    - `recipientName` — `String`

</details>

<details>
<summary><code>GET /api/v1/stats/antibiograms</code></summary>

- **Permission:** `stats.read`
- **Success:** 200 OK
- **Query params:**
  - `from` — `LocalDate`, required
  - `to` — `LocalDate`, required
  - `disciplineId` — `String`, optional
- **Response body:** `AntibiogramsResponse`
  - `rows` — `List<Row>`
    - `dailyId` — `String`
    - `code` — `String`
    - `analysisName` — `String`
    - `recipientName` — `String`

</details>

<details>
<summary><code>GET /api/v1/stats/by-discipline</code></summary>

- **Permission:** `stats.read`
- **Success:** 200 OK
- **Query params:**
  - `from` — `LocalDate`, required
  - `to` — `LocalDate`, required
  - `insuranceId` — `String`, optional
  - `partnerId` — `String`, optional
- **Response body:** `ByDisciplineResponse`
  - `rows` — `List<Row>`
    - `dailyId` — `String`
    - `code` — `String`
    - `analysisName` — `String`
    - `recipientName` — `String`

</details>

<details>
<summary><code>GET /api/v1/stats/by-prescriber</code></summary>

- **Permission:** `stats.read`
- **Success:** 200 OK
- **Query params:**
  - `from` — `LocalDate`, required
  - `to` — `LocalDate`, required
  - `disciplineId` — `String`, optional
- **Response body:** `ByPrescriberResponse`
  - `rows` — `List<Row>`
    - `dailyId` — `String`
    - `code` — `String`
    - `analysisName` — `String`
    - `recipientName` — `String`

</details>

<details>
<summary><code>GET /api/v1/stats/export</code></summary>

- **Permission:** `stats.read`
- **Success:** 200 OK
- **Produces:** varies — read the `Content-Type` response header
- **Download:** binary body with `Content-Disposition: attachment; filename="…"`
- **Query params:**
  - `from` — `LocalDate`, required
  - `to` — `LocalDate`, required
  - `format` — `String`, required
- **Response body:** `byte[]`

</details>

<details>
<summary><code>GET /api/v1/stats/patients</code></summary>

- **Permission:** `stats.read`
- **Success:** 200 OK
- **Query params:**
  - `from` — `LocalDate`, required
  - `to` — `LocalDate`, required
  - `insuranceId` — `String`, optional
  - `partnerId` — `String`, optional
- **Response body:** `PatientStatsResponse`
  - `bySex` — `Map<String, Long>`
  - `byAgeBand` — `Map<String, Long>`
  - `newCount` — `long`
  - `returningCount` — `long`
  - `allCount` — `long`

</details>


### Laboratory settings

| Method | Path | Permission | Success |
|---|---|---|---|
| `GET` | `/api/v1/lab-settings` | `lab-settings.read` | 200 OK |
| `PUT` | `/api/v1/lab-settings` | `lab-settings.manage` | 200 OK |

<details>
<summary><code>GET /api/v1/lab-settings</code></summary>

- **Permission:** `lab-settings.read`
- **Success:** 200 OK
- **Response body:** `LabSettings`
  - `name` — `String`
  - `address` — `String`
  - `accreditationNumber` — `String`
  - `managerName` — `String`
  - `updatedAt` — `Instant`

</details>

<details>
<summary><code>PUT /api/v1/lab-settings</code></summary>

- **Permission:** `lab-settings.manage`
- **Success:** 200 OK
- **Request body:** `LabSettingsUpdateRequest`
  - `name` — `String`
  - `address` — `String`
  - `accreditationNumber` — `String`
  - `managerName` — `String`
- **Response body:** `LabSettings`
  - `name` — `String`
  - `address` — `String`
  - `accreditationNumber` — `String`
  - `managerName` — `String`
  - `updatedAt` — `Instant`

</details>


### Async PDF jobs

| Method | Path | Permission | Success |
|---|---|---|---|
| `GET` | `/api/v1/pdf-jobs/{id}` | `pdf.read-own-jobs` | 200 OK |

<details>
<summary><code>GET /api/v1/pdf-jobs/{id}</code></summary>

- **Permission:** `pdf.read-own-jobs`
- **Success:** 200 OK
- **Produces:** application/pdf
- **Download:** binary body with `Content-Disposition: attachment; filename="…"`
- **Path params:** `id` (`String`)
- **Query params:**
  - `download` — `boolean`, optional, default `false`

</details>


---

## Appendix A — Permission → role matrix

Seeded by Liquibase (`db/changelog/data/`). `ADM`=ADMIN, `LMG`=LAB_MANAGER, `BIO`=BIOLOGIST, `TEC`=TECHNICIAN, `REC`=RECEPTIONIST, `CSH`=CASHIER, `ACC`=ACCOUNTANT.

| Permission | ADM | LMG | BIO | TEC | REC | CSH | ACC | What it unlocks |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|---|
| `accounting.read` | ● | ● |  |  |  | ● | ● | Read accounting recap, by-payment-mode, and outstanding views |
| `audit.checkpoint` | ● |  |  |  |  |  |  | Créer un point de contrôle d'audit |
| `audit.read` | ● |  |  |  |  |  |  | Read the audit trail |
| `cash-register.manage` | ● | ● |  |  |  |  |  | Manage cash registers and user assignments |
| `cashbox.close-day` | ● | ● |  |  |  | ● |  | Close the cashbox for the day |
| `cashbox.expense-category.manage` |  | ● |  |  |  |  |  | Manage cashbox expense-category catalog |
| `cashbox.expense-category.read` |  | ● | ● |  | ● | ● | ● | Read cashbox expense-category catalog |
| `cashbox.expense.read` |  | ● |  |  | ● | ● | ● | Read cashbox expenses |
| `cashbox.expense.record` |  | ● |  |  | ● | ● |  | Record a cashbox expense |
| `cashbox.home-collection.read` |  | ● |  |  | ● | ● | ● | Read cashbox home-collection fee records |
| `cashbox.home-collection.record` |  | ● |  |  | ● | ● |  | Record a cashbox home-collection fee |
| `cashbox.journal.read` |  | ● |  |  | ● | ● | ● | Read cashbox journal des caisses |
| `cashbox.session.close` |  | ● |  |  | ● | ● |  | Close a cashbox session |
| `cashbox.session.reopen` |  | ● |  |  |  |  |  | Reopen a closed cashbox session |
| `cashbox.use-assigned` | ● | ● |  |  | ● | ● |  | Use the cashbox assigned to this user |
| `catalog.manage` | ● | ● |  |  |  |  |  | Manage analysis / bilan / pricing catalog |
| `catalog.view-history` | ● | ● |  |  |  |  |  | — |
| `dossier.print` | ● |  | ● |  | ● |  |  | Marquer un dossier comme imprimé |
| `dossier.publish` | ● |  | ● |  |  |  |  | Publish dossier (online + email) |
| `dossier.send` | ● |  | ● |  |  |  |  | Send a published dossier to the patient by email |
| `dossier.sign-electronically` | ● |  | ● |  |  |  |  | Sign a dossier electronically |
| `expense.record` | ● |  |  |  |  | ● | ● | Record an expense |
| `home-collection-fee.record` | ● |  |  |  |  | ● |  | Record home-collection fee |
| `insurance.manage` | ● | ● |  |  |  |  |  | Manage insurance partners |
| `invoice.create` | ● |  |  |  | ● |  |  | Create an invoice |
| `invoice.read` | ● | ● |  |  | ● | ● | ● | Read invoices |
| `lab-file.create` | ● |  | ● | ● | ● |  |  | Open a new lab file (dossier d'analyse) |
| `lab-file.read` | ● |  | ● | ● | ● |  |  | Read an existing lab file |
| `lab-settings.manage` | ● |  |  |  |  |  |  | Update the lab identity values |
| `lab-settings.read` | ● |  | ● | ● | ● |  |  | Read the lab identity (name, address, accreditation, manager) |
| `partner.manage` | ● | ● |  |  |  |  |  | Manage external partner labs |
| `patient.create` | ● | ● |  |  | ● |  |  | Register a new patient |
| `patient.merge` | ● | ● |  |  | ● |  |  | Merge duplicate patient records |
| `patient.search` | ● | ● |  |  | ● |  |  | Search the patient registry |
| `patient.update` | ● | ● |  |  | ● |  |  | Update patient identity / contact |
| `payment.collect` | ● |  |  |  | ● | ● |  | Collect payment |
| `pdf.read-own-jobs` | ● |  | ● | ● | ● |  |  | Read own PDF render jobs (poll /pdf-jobs/{id}) |
| `prescriber.manage` | ● | ● |  |  |  |  |  | Manage prescribers (médecins) |
| `quote.create` | ● |  |  |  | ● |  |  | Create a quote (devis) |
| `quote.lock` | ● |  |  |  | ● |  |  | Lock a quote against further changes |
| `quote.read` | ● |  |  |  |  |  |  | Read quotes (list/detail/search) |
| `quote.send` | ● |  |  |  | ● |  |  | Send a quote to a client |
| `result.enter` | ● |  |  | ● |  |  |  | Enter analysis results |
| `result.validate-bio` | ● |  | ● |  |  |  |  | Biological (clinical) validation of a result |
| `result.validate-tech` | ● |  |  | ● |  |  |  | Technical validation of a result |
| `sample.destroy` | ● |  | ● | ● |  |  |  | Marquer un échantillon comme détruit |
| `sample.record-external` | ● |  |  | ● | ● |  |  | Record sample brought in from outside |
| `sample.record-home` | ● |  |  | ● | ● |  |  | Record sample collected at home |
| `sample.record-in-lab` | ● |  |  | ● | ● |  |  | Record sample collected in lab |
| `stats.read` | ● | ● | ● |  |  |  | ● | Read statistics dashboard |
| `users.manage` | ● | ● |  |  |  |  |  | Manage users and role assignments |

## Appendix B — Enumerations

Every enum is serialised as its **name string** (e.g. `"MONSIEUR"`), never an ordinal.

- **`AgeUnit`** — `DAYS`, `MONTHS`, `YEARS`
- **`AnalysisKind`** — `SIMPLE`, `GROUPED`, `SOCIETY_PANEL`
- **`AttachableType`** — `LAB_FILE`, `EXPENSE`, `HOME_COLLECTION_FEE`
- **`Backend`** — `FILESYSTEM`, `S3`
- **`Channel`** — `EMAIL`, `WHATSAPP`
- **`Civility`** — `MONSIEUR`, `MADAME`, `MADEMOISELLE`, `ENFANT`, `NOUVEAU_NE`
- **`ContextType`** — `QUOTE`, `LAB_FILE`, `SAMPLE`
- **`DeliveryStatus`** — `PENDING`, `SENT`, `FAILED`, `ABANDONED`
- **`DisciplinePrefix`** — `BIOCHIMIE`, `HEMATOLOGIE`, `IMMUNO_SEROLOGIE`, `MICROBIOLOGIE`, `BIOLOGIE_MOLECULAIRE`
- **`DiscountKind`** — `AMOUNT`, `PERCENT`
- **`DossierMarker`** — `GREEN`, `RED`, `PEN`
- **`DossierState`** — `VALIDATED`, `NOT_VALIDATED`, `IN_PROGRESS`, `DELETED`
- **`EntityType`** — `PRESCRIBER`, `PRELEVEUR`
- **`GuardianRelationship`** — `PARENT`, `TUTEUR`, `ACCOMPAGNANT`, `AUTRE`
- **`Habilitation`** — `TOUT_LE_MONDE`, `ADULTES_VEINES_VISIBLES`, `ADULTES_VEINES_PROFONDES_OU_FINES`, `PERSONNES_EN_FORME`, `ENFANTS_VEINES_VISIBLES`, `ENFANTS_VEINES_FINES`, `NOURRISSON_NOUVEAU_NE`
- **`IdDocumentType`** — `CARTE_IDENTITE`, `PASSEPORT`, `PERMIS_CONDUIRE`, `AUTRE`
- **`InvoiceCustomerKind`** — `PATIENT`, `INSURANCE`, `PARTNER`
- **`LabCountry`** — `TOGO`, `BENIN`
- **`LabFileStatus`** — `OPEN`, `AWAITING_RESULTS`, `TECHNICALLY_VALIDATED`, `BIOLOGICALLY_VALIDATED`, `SIGNED`, `PUBLISHED`
- **`OfficeFormat`** — `DOCX`, `XLSX`
- **`OperationKind`** — `PAYMENT`, `EXPENSE`, `HOME_COLLECTION_FEE`
- **`PageSize`** — `A4_PORTRAIT`, `A4_LANDSCAPE`, `A5_PORTRAIT`
- **`PartnerKind`** — `SOCIETY`, `PARTNER_LAB`, `HEALTH_CENTER`
- **`PatientCategory`** — `HOMME`, `FEMME`, `ENFANT`, `NOURRISSON_NOUVEAU_NE`
- **`PatientType`** — `INSURED`, `NOT_INSURED`, `SOCIETY`, `PARTNER_LAB`
- **`PaymentMode`** — `CASH`, `CHEQUE`, `CARD_TPE`, `FLOOZ`, `MIXX_BY_YAS`
- **`PdfRenderJobStatus`** — `QUEUED`, `RUNNING`, `DONE`, `FAILED`
- **`QuoteStatus`** — `DRAFT`, `LOCKED`, `CONVERTED`, `EXPIRED`
- **`ReferenceMode`** — `STANDARD`, `COMMENT`, `NONE`
- **`ResultState`** — `DRAFT`, `ENTERED`, `TECHNICAL_VALIDATED`, `BIOLOGICAL_VALIDATED`
- **`ResultType`** — `FORMULA`, `INTEGER`, `REAL`, `TEXT`, `FREE`
- **`Role`** — `ADMIN`, `LAB_MANAGER`, `BIOLOGIST`, `TECHNICIAN`, `RECEPTIONIST`, `CASHIER`, `ACCOUNTANT`
- **`SampleType`** — `IN_LAB`, `HOME`, `EXTERNAL_RECEIVED`
- **`Scope`** — `ANALYSIS`, `REFERENCE_INTERVALS`, `COMPONENTS`, `TECHNIQUES`
- **`SearchResultState`** — `EMPTY`, `TO_CHECK`, `URGENT`, `VALIDATED`, `NOT_VALIDATED`
- **`Sensitivity`** — `R`, `I`, `S`
- **`SessionStatus`** — `OPEN`, `CLOSED`
- **`Sex`** — `M`, `F`

## Appendix C — Endpoint index

All 188 endpoints, alphabetical by path.

| Method | Path | Permission |
|---|---|---|
| `GET` | `/api/v1/accounting/by-payment-mode` | `accounting.read` |
| `GET` | `/api/v1/accounting/daily` | `accounting.read` |
| `GET` | `/api/v1/accounting/outstanding` | `accounting.read` |
| `GET` | `/api/v1/admin/users` | `users.manage` |
| `POST` | `/api/v1/admin/users` | `users.manage` |
| `DELETE` | `/api/v1/admin/users/{id}` | `users.manage` |
| `GET` | `/api/v1/admin/users/{id}` | `users.manage` |
| `PATCH` | `/api/v1/admin/users/{id}` | `users.manage` |
| `PUT` | `/api/v1/admin/users/{id}/roles` | `users.manage` |
| `GET` | `/api/v1/audit/actions` | `audit.read` |
| `POST` | `/api/v1/audit/checkpoints` | `audit.checkpoint` |
| `GET` | `/api/v1/audit/checkpoints/latest` | `audit.read` |
| `GET` | `/api/v1/audit/lab-files/{labFileId}/actions` | `audit.read` |
| `POST` | `/api/v1/audit/verify` | `audit.read` |
| `GET` | `/api/v1/cash-registers` | `cash-register.manage` |
| `POST` | `/api/v1/cash-registers` | `cash-register.manage` |
| `DELETE` | `/api/v1/cash-registers/{id}` | `cash-register.manage` |
| `GET` | `/api/v1/cash-registers/{id}` | `cash-register.manage` |
| `PUT` | `/api/v1/cash-registers/{id}` | `cash-register.manage` |
| `PUT` | `/api/v1/cash-registers/{id}/users` | `cash-register.manage` |
| `GET` | `/api/v1/cashbox/expense-categories` | `cashbox.expense-category.read` |
| `POST` | `/api/v1/cashbox/expense-categories` | `cashbox.expense-category.manage` |
| `DELETE` | `/api/v1/cashbox/expense-categories/{id}` | `cashbox.expense-category.manage` |
| `PATCH` | `/api/v1/cashbox/expense-categories/{id}` | `cashbox.expense-category.manage` |
| `GET` | `/api/v1/cashbox/expenses` | `cashbox.expense.read` |
| `POST` | `/api/v1/cashbox/expenses` | `cashbox.expense.record` |
| `GET` | `/api/v1/cashbox/expenses/{expenseId}/attachments` | `cashbox.expense.read` |
| `POST` | `/api/v1/cashbox/expenses/{expenseId}/attachments` | `cashbox.expense.record` |
| `DELETE` | `/api/v1/cashbox/expenses/{expenseId}/attachments/{attachmentId}` | `cashbox.expense.record` |
| `GET` | `/api/v1/cashbox/expenses/{expenseId}/attachments/{attachmentId}` | `cashbox.expense.read` |
| `GET` | `/api/v1/cashbox/home-collection-fees` | `cashbox.home-collection.read` |
| `POST` | `/api/v1/cashbox/home-collection-fees` | `cashbox.home-collection.record` |
| `GET` | `/api/v1/cashbox/home-collection-fees/{feeId}/attachments` | `cashbox.home-collection.read` |
| `POST` | `/api/v1/cashbox/home-collection-fees/{feeId}/attachments` | `cashbox.home-collection.record` |
| `DELETE` | `/api/v1/cashbox/home-collection-fees/{feeId}/attachments/{attachmentId}` | `cashbox.home-collection.record` |
| `GET` | `/api/v1/cashbox/home-collection-fees/{feeId}/attachments/{attachmentId}` | `cashbox.home-collection.read` |
| `GET` | `/api/v1/cashbox/journal` | `cashbox.journal.read` |
| `GET` | `/api/v1/cashbox/sessions/{cashRegisterId}/{sessionDate}` | `cashbox.journal.read` |
| `POST` | `/api/v1/cashbox/sessions/{cashRegisterId}/{sessionDate}:close` | `cashbox.session.close` |
| `POST` | `/api/v1/cashbox/sessions/{cashRegisterId}/{sessionDate}:reopen` | `cashbox.session.reopen` |
| `GET` | `/api/v1/catalog/analyses` | `catalog.manage` |
| `POST` | `/api/v1/catalog/analyses` | `catalog.manage` |
| `DELETE` | `/api/v1/catalog/analyses/{id}` | `catalog.manage` |
| `GET` | `/api/v1/catalog/analyses/{id}` | `catalog.manage` |
| `PATCH` | `/api/v1/catalog/analyses/{id}` | `catalog.manage` |
| `POST` | `/api/v1/catalog/analyses/{id}/components` | `catalog.manage` |
| `DELETE` | `/api/v1/catalog/analyses/{id}/components/{childCode}` | `catalog.manage` |
| `GET` | `/api/v1/catalog/analyses/{id}/history` | `catalog.view-history` |
| `PUT` | `/api/v1/catalog/analyses/{id}/intervals` | `catalog.manage` |
| `POST` | `/api/v1/catalog/analyses/{id}/intervals:replicate` | `catalog.manage` |
| `PUT` | `/api/v1/catalog/analyses/{id}/techniques` | `catalog.manage` |
| `POST` | `/api/v1/catalog/analyses/{id}:convert-to-grouped` | `catalog.manage` |
| `POST` | `/api/v1/catalog/analyses/{id}:convert-to-simple` | `catalog.manage` |
| `POST` | `/api/v1/catalog/analyses/{id}:duplicate` | `catalog.manage` |
| `GET` | `/api/v1/catalog/antibiotics` | `catalog.manage` |
| `POST` | `/api/v1/catalog/antibiotics` | `catalog.manage` |
| `DELETE` | `/api/v1/catalog/antibiotics/{id}` | `catalog.manage` |
| `GET` | `/api/v1/catalog/antibiotics/{id}` | `catalog.manage` |
| `PATCH` | `/api/v1/catalog/antibiotics/{id}` | `catalog.manage` |
| `GET` | `/api/v1/catalog/disciplines` | `catalog.manage` |
| `POST` | `/api/v1/catalog/disciplines` | `catalog.manage` |
| `DELETE` | `/api/v1/catalog/disciplines/{id}` | `catalog.manage` |
| `GET` | `/api/v1/catalog/disciplines/{id}` | `catalog.manage` |
| `PATCH` | `/api/v1/catalog/disciplines/{id}` | `catalog.manage` |
| `GET` | `/api/v1/catalog/matrices` | `catalog.manage` |
| `POST` | `/api/v1/catalog/matrices` | `catalog.manage` |
| `DELETE` | `/api/v1/catalog/matrices/{id}` | `catalog.manage` |
| `GET` | `/api/v1/catalog/matrices/{id}` | `catalog.manage` |
| `PATCH` | `/api/v1/catalog/matrices/{id}` | `catalog.manage` |
| `GET` | `/api/v1/catalog/preleveurs` | `catalog.manage` |
| `POST` | `/api/v1/catalog/preleveurs` | `catalog.manage` |
| `DELETE` | `/api/v1/catalog/preleveurs/{id}` | `catalog.manage` |
| `GET` | `/api/v1/catalog/preleveurs/{id}` | `catalog.manage` |
| `PATCH` | `/api/v1/catalog/preleveurs/{id}` | `catalog.manage` |
| `GET` | `/api/v1/catalog/prescribers` | `prescriber.manage` |
| `POST` | `/api/v1/catalog/prescribers` | `prescriber.manage` |
| `DELETE` | `/api/v1/catalog/prescribers/{id}` | `prescriber.manage` |
| `GET` | `/api/v1/catalog/prescribers/{id}` | `prescriber.manage` |
| `PATCH` | `/api/v1/catalog/prescribers/{id}` | `prescriber.manage` |
| `GET` | `/api/v1/catalog/recipients` | `catalog.manage` |
| `POST` | `/api/v1/catalog/recipients` | `catalog.manage` |
| `DELETE` | `/api/v1/catalog/recipients/{id}` | `catalog.manage` |
| `GET` | `/api/v1/catalog/recipients/{id}` | `catalog.manage` |
| `PATCH` | `/api/v1/catalog/recipients/{id}` | `catalog.manage` |
| `GET` | `/api/v1/catalog/specialties` | `prescriber.manage` |
| `POST` | `/api/v1/catalog/specialties` | `prescriber.manage` |
| `DELETE` | `/api/v1/catalog/specialties/{id}` | `prescriber.manage` |
| `GET` | `/api/v1/catalog/specialties/{id}` | `prescriber.manage` |
| `PATCH` | `/api/v1/catalog/specialties/{id}` | `prescriber.manage` |
| `GET` | `/api/v1/catalog/techniques` | `catalog.manage` |
| `POST` | `/api/v1/catalog/techniques` | `catalog.manage` |
| `DELETE` | `/api/v1/catalog/techniques/{id}` | `catalog.manage` |
| `GET` | `/api/v1/catalog/techniques/{id}` | `catalog.manage` |
| `PATCH` | `/api/v1/catalog/techniques/{id}` | `catalog.manage` |
| `GET` | `/api/v1/dossiers` | `lab-file.read` |
| `GET` | `/api/v1/insurances` | `lab-file.read` |
| `POST` | `/api/v1/insurances` | `insurance.manage` |
| `DELETE` | `/api/v1/insurances/{id}` | `insurance.manage` |
| `GET` | `/api/v1/insurances/{id}` | `lab-file.read` |
| `PUT` | `/api/v1/insurances/{id}` | `insurance.manage` |
| `GET` | `/api/v1/invoices` | `invoice.read` |
| `POST` | `/api/v1/invoices` | `invoice.create` |
| `GET` | `/api/v1/invoices/{id}` | `invoice.read` |
| `GET` | `/api/v1/lab-files` | `lab-file.read` |
| `POST` | `/api/v1/lab-files` | `lab-file.create` |
| `GET` | `/api/v1/lab-files/queues/overdue` | `lab-file.read` |
| `GET` | `/api/v1/lab-files/queues/urgent` | `lab-file.read` |
| `DELETE` | `/api/v1/lab-files/{id}` | `lab-file.create` |
| `GET` | `/api/v1/lab-files/{id}` | `lab-file.read` |
| `PATCH` | `/api/v1/lab-files/{id}` | `lab-file.create` |
| `GET` | `/api/v1/lab-files/{id}/export` | `lab-file.read` |
| `GET` | `/api/v1/lab-files/{id}/labels` | `lab-file.read` |
| `PUT` | `/api/v1/lab-files/{id}/rendezvous` | `lab-file.create` |
| `GET` | `/api/v1/lab-files/{id}/worksheet.pdf` | `lab-file.read` |
| `POST` | `/api/v1/lab-files/{id}:mark-printed` | `dossier.print` |
| `POST` | `/api/v1/lab-files/{id}:publish` | `dossier.publish` |
| `POST` | `/api/v1/lab-files/{id}:send` | `dossier.send` |
| `POST` | `/api/v1/lab-files/{id}:sign` | `dossier.sign-electronically` |
| `POST` | `/api/v1/lab-files/{labFileId}/analyses` | `lab-file.create` |
| `DELETE` | `/api/v1/lab-files/{labFileId}/analyses/{requestId}` | `lab-file.create` |
| `GET` | `/api/v1/lab-files/{labFileId}/attachments` | `lab-file.read` |
| `POST` | `/api/v1/lab-files/{labFileId}/attachments` | `lab-file.create` |
| `DELETE` | `/api/v1/lab-files/{labFileId}/attachments/{attachmentId}` | `lab-file.create` |
| `GET` | `/api/v1/lab-files/{labFileId}/attachments/{attachmentId}` | `lab-file.read` |
| `DELETE` | `/api/v1/lab-files/{labFileId}/dossier-comment` | `result.enter` |
| `GET` | `/api/v1/lab-files/{labFileId}/dossier-comment` | `result.enter` |
| `PUT` | `/api/v1/lab-files/{labFileId}/dossier-comment` | `result.enter` |
| `GET` | `/api/v1/lab-files/{labFileId}/payments` | `invoice.read` |
| `POST` | `/api/v1/lab-files/{labFileId}/samples:external` | `sample.record-external` |
| `POST` | `/api/v1/lab-files/{labFileId}/samples:home` | `sample.record-home` |
| `POST` | `/api/v1/lab-files/{labFileId}/samples:in-lab` | `sample.record-in-lab` |
| `GET` | `/api/v1/lab-settings` | `lab-settings.read` |
| `PUT` | `/api/v1/lab-settings` | `lab-settings.manage` |
| `GET` | `/api/v1/me` | — |
| `PATCH` | `/api/v1/me/password` | — |
| `GET` | `/api/v1/paillasses/{disciplineId}/worksheet.pdf` | `lab-file.read` |
| `GET` | `/api/v1/partners` | `lab-file.read` |
| `POST` | `/api/v1/partners` | `partner.manage` |
| `DELETE` | `/api/v1/partners/{id}` | `partner.manage` |
| `GET` | `/api/v1/partners/{id}` | `lab-file.read` |
| `PUT` | `/api/v1/partners/{id}` | `partner.manage` |
| `GET` | `/api/v1/patients` | `patient.search` |
| `POST` | `/api/v1/patients` | `patient.create` |
| `DELETE` | `/api/v1/patients/{id}` | `patient.update` |
| `GET` | `/api/v1/patients/{id}` | `patient.search` |
| `PATCH` | `/api/v1/patients/{id}` | `patient.update` |
| `POST` | `/api/v1/patients/{id}/merge` | `patient.merge` |
| `POST` | `/api/v1/payments` | `payment.collect` |
| `GET` | `/api/v1/payments/{id}/receipt.pdf` | `invoice.read` |
| `GET` | `/api/v1/pdf-jobs/{id}` | `pdf.read-own-jobs` |
| `GET` | `/api/v1/portal/dossiers` | — |
| `GET` | `/api/v1/portal/dossiers/{labFileId}/pdf` | — |
| `POST` | `/api/v1/portal/login` | — |
| `GET` | `/api/v1/quotes` | `quote.read` |
| `POST` | `/api/v1/quotes` | `quote.create` |
| `DELETE` | `/api/v1/quotes/{id}` | `quote.create` |
| `GET` | `/api/v1/quotes/{id}` | `quote.read` |
| `PATCH` | `/api/v1/quotes/{id}` | `quote.create` |
| `GET` | `/api/v1/quotes/{id}/export` | `quote.read` |
| `POST` | `/api/v1/quotes/{id}/lines` | `quote.create` |
| `DELETE` | `/api/v1/quotes/{id}/lines/{lineId}` | `quote.create` |
| `GET` | `/api/v1/quotes/{id}/pdf` | `quote.read` |
| `POST` | `/api/v1/quotes/{id}:convert` | `quote.create` |
| `POST` | `/api/v1/quotes/{id}:lock` | `quote.lock` |
| `POST` | `/api/v1/quotes/{id}:send` | `quote.send` |
| `GET` | `/api/v1/results` | `result.enter` |
| `GET` | `/api/v1/results/anterior` | `result.enter` |
| `GET` | `/api/v1/results/queues/to-print` | `result.validate-bio` |
| `GET` | `/api/v1/results/queues/to-validate` | `result.validate-bio` |
| `GET` | `/api/v1/results/search` | `result.enter` |
| `DELETE` | `/api/v1/results/{id}` | `result.enter` |
| `GET` | `/api/v1/results/{id}` | `result.enter` |
| `POST` | `/api/v1/results/{id}/antibiogram` | `result.enter` |
| `DELETE` | `/api/v1/results/{id}/antibiogram/{readingId}` | `result.enter` |
| `PATCH` | `/api/v1/results/{id}/lines/{lineId}` | `result.enter` |
| `DELETE` | `/api/v1/results/{id}/lines/{lineId}/value` | `result.enter` |
| `POST` | `/api/v1/results/{id}:validate-biological` | `result.validate-bio` |
| `POST` | `/api/v1/results/{id}:validate-technical` | `result.validate-tech` |
| `GET` | `/api/v1/results/{labFileId}/pdf` | `lab-file.read` |
| `GET` | `/api/v1/samples/queues/destruction-due` | `lab-file.read` |
| `POST` | `/api/v1/samples/{id}:mark-destroyed` | `sample.destroy` |
| `GET` | `/api/v1/stats/activity` | `stats.read` |
| `GET` | `/api/v1/stats/analyses` | `stats.read` |
| `GET` | `/api/v1/stats/antibiograms` | `stats.read` |
| `GET` | `/api/v1/stats/by-discipline` | `stats.read` |
| `GET` | `/api/v1/stats/by-prescriber` | `stats.read` |
| `GET` | `/api/v1/stats/export` | `stats.read` |
| `GET` | `/api/v1/stats/patients` | `stats.read` |
