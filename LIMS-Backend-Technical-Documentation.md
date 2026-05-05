# Laboratory Information Management System (LIMS)
## Backend Technical Documentation

**Stack:** Java 25 · Spring Boot 4.0.x · PostgreSQL · Maven
**Author:** Samuel
**Last updated:** 2026-05-03

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [High-Level Architecture](#3-high-level-architecture)
4. [Project Structure](#4-project-structure)
5. [Domain Model](#5-domain-model)
6. [Database Design](#6-database-design)
7. [REST API Design](#7-rest-api-design)
8. [Module Implementation Details](#8-module-implementation-details)
9. [Security & Authentication](#9-security--authentication)
10. [Cross-Cutting Concerns](#10-cross-cutting-concerns)
11. [File & Document Handling](#11-file--document-handling)
12. [Notifications (Mail / WhatsApp / SMS)](#12-notifications-mail--whatsapp--sms)
13. [Reporting & Statistics](#13-reporting--statistics)
14. [Integrations](#14-integrations)
15. [Testing Strategy](#15-testing-strategy)
16. [Observability](#16-observability)
17. [Deployment & DevOps](#17-deployment--devops)
18. [Performance & Scalability](#18-performance--scalability)
19. [Security Hardening Checklist](#19-security-hardening-checklist)
20. [Implementation Roadmap](#20-implementation-roadmap)

---

## 1. Project Overview

### 1.1 Purpose
Build the backend of a Laboratory Information Management System (LIMS) for medical laboratories operating in West Africa (Togo, Benin). The backend exposes a REST API consumed by a desktop or web frontend and covers the full laboratory workflow: patient registration, sample collection, analysis ordering, result entry, biological validation, billing, accounting, and statistics.

### 1.2 Business Scope
The system must handle:
- Patient reception and file management
- Quotes and invoicing (with insurance and partner contracts)
- Sample collection (in lab, at home, externally received)
- Analysis configuration (simple, grouped, society panels) by discipline
- Result entry with reference intervals (age/sex/category) and antibiogram interpretation
- Two-step result validation (technical + biological)
- Result delivery (print, email, online portal)
- Cash registers, daily journals, expenses
- Statistics across prescribers, disciplines, patients, insurances

### 1.3 Non-Functional Requirements

| Concern | Target |
|---|---|
| Concurrent users per lab | 20–50 |
| Daily files per lab | 100–500 |
| Response time (p95) | < 300 ms for CRUD, < 1 s for reports |
| Availability | 99.5% during business hours |
| Data retention | 10 years (medical records) |
| Backup | Daily full + 15-min WAL archive |
| Audit | Full traceability of all writes on patient/result data |
| Compliance | OMS Good Laboratory Practice, ISO 15189 alignment |

---

## 2. Technology Stack

### 2.1 Core
- **Java 25** (LTS) — leveraging records, sealed types, pattern matching, virtual threads, text blocks
- **Spring Boot 4.0.x** — application framework
- **Spring Web MVC** — REST endpoints (with virtual-thread executor)
- **Spring Data JPA / Hibernate 6** — persistence
- **Spring Security 6** — authentication and authorization
- **Spring Validation** — Jakarta Bean Validation 3
- **Spring Modulith** — modular monolith with verified module boundaries

### 2.2 Database & Cache
- **PostgreSQL 17** — primary RDBMS
- **Liquibase** — schema migration with versioned changelogs in YAML, contexts per environment, rollback support, drift detection
- **HikariCP** — connection pool (default in Spring Boot)
- **Redis** (optional, phase 2) — session store, rate limiting, lookup caches

### 2.3 Build & Tooling
- **Maven** with the Spring Boot parent POM
- **Lombok** — `@Slf4j`, `@RequiredArgsConstructor`, `@Getter`, `@Setter`, `@Builder` allowed; `@Data` and `@SneakyThrows` forbidden. Records remain preferred for value objects; JPA entities stay plain classes.
- **MapStruct** — DTO ↔ entity mapping
- **Testcontainers** — integration tests against a real PostgreSQL
- **Spotless / Checkstyle** — formatting and lint

### 2.4 API & Documentation
- **springdoc-openapi** — OpenAPI 3 / Swagger UI
- **Problem Details (RFC 7807)** — error representation

### 2.5 Observability
- **Micrometer + Prometheus** — metrics
- **Logback + JSON encoder** — structured logs
- **OpenTelemetry** — distributed tracing
- **Spring Boot Actuator** — health, info, metrics endpoints

### 2.6 Messaging & Background Work
- **Spring Events** (in-process) for module-to-module signaling
- **Quartz Scheduler** or `@Scheduled` for cron jobs (sample destruction alerts, overdue reports)
- **Java 25 Virtual Threads** for I/O-bound work; pinned platform threads for JNI/file scans

### 2.7 Document & Reporting
- **Apache POI** — XLSX export
- **OpenPDF / iText 7 (community)** — PDF generation (invoices, lab reports)
- **docx4j** — DOCX export of result files
- **JasperReports** (optional) — complex statistics reports

---

## 3. High-Level Architecture

### 3.1 Style
A **modular monolith** built with **Spring Modulith**, organized along the bounded contexts identified in the spec. Each module owns its tables and exposes a service API; cross-module reads go through API calls or domain events, never direct repository access.

This style fits the scope of a single-lab deployment, simplifies operations, and leaves a clear path to extract microservices later (billing, statistics, and notifications are the most likely candidates).

### 3.2 Layering inside a module

```
┌──────────────────────────────────────────────────────────┐
│                       Web layer                          │
│   Controllers · DTOs · Validation · OpenAPI · Mappers    │
├──────────────────────────────────────────────────────────┤
│                   Application layer                      │
│   Use-case services · Transactions · Authorization       │
├──────────────────────────────────────────────────────────┤
│                     Domain layer                         │
│   Entities · Value Objects · Domain services · Events    │
├──────────────────────────────────────────────────────────┤
│                 Infrastructure layer                     │
│   JPA repositories · External clients · File adapters    │
└──────────────────────────────────────────────────────────┘
```

### 3.3 Modules (Spring Modulith)

| Module | Responsibility |
|---|---|
| `iam` | Users, roles, permissions, authentication, audit |
| `patient` | Patient identity, file lookup, merge, contacts |
| `catalog` | Analyses (simple/grouped), disciplines, matrices, techniques, recipients, antibiotics, prescribers |
| `order` | Lab files (dossiers), quotes, sample collection events |
| `result` | Result entry, technical/biological validation, reference intervals, antibiogram interpretation |
| `billing` | Invoices, insurance pricing, partner contracts, society panels |
| `cashbox` | Registers, daily journals, payments, expenses, home-collection fees |
| `delivery` | Result distribution: print, email, online link |
| `stats` | Aggregated reporting and KPIs |
| `notification` | Outbound mail / WhatsApp / SMS |
| `attachment` | Scanned documents and lab-report file storage |

### 3.4 Cross-cutting Adapters
- `security` — JWT issuing, password hashing, MFA
- `audit` — Hibernate Envers + application-level event log
- `config` — global Spring configuration

---

## 4. Project Structure

```
lims-backend/
├── pom.xml
├── Jenkinsfile
├── .mvn/
├── src/main/java/org/sparkcorporation/lims/
│   ├── LimsApplication.java
│   ├── shared/
│   │   ├── domain/         # Money, Code, Identifier, RangeOfDays...
│   │   ├── web/            # ProblemDetail handlers, paging
│   │   ├── security/
│   │   └── audit/
│   ├── iam/
│   │   ├── api/            # public service interfaces (consumed by other modules)
│   │   ├── domain/
│   │   ├── infra/
│   │   └── web/
│   ├── patient/...
│   ├── catalog/...
│   ├── order/...
│   ├── result/...
│   ├── billing/...
│   ├── cashbox/...
│   ├── delivery/...
│   ├── stats/...
│   ├── notification/...
│   └── attachment/...
├── src/main/resources/
│   ├── application.yml
│   ├── application-prod.yml
│   ├── db/changelog/       # Liquibase: db.changelog-master.yaml + per-version files
│   └── templates/          # email & report templates
└── src/test/java/...
```

Each module (e.g. `patient`) follows the same internal layout: `api`, `domain`, `infra`, `web`. The `api` package contains only interfaces and DTOs; everything else inside the module is package-private. Spring Modulith verifies this at test time with `ApplicationModules.of(...).verify()`.

---

## 5. Domain Model

### 5.1 Aggregate Roots (per module)

| Module | Aggregate | Key fields |
|---|---|---|
| `iam` | `User` | username, hashedPassword, roles, cashRegisterId |
| `patient` | `Patient` | labCode, names, civility, sex, idDocument, contacts |
| `catalog` | `Analysis` (simple/grouped), `Discipline`, `Matrix`, `Technique`, `Antibiotic`, `Prescriber` | code, name, indiciaryValue (B), recipient, matrix, ranges |
| `order` | `LabFile` | registrationNumber, patient, prescriber, urgency, samples |
| `result` | `ResultEntry` | analysis, value, unit, refRange, technicalValidation, biologicalValidation |
| `billing` | `Invoice` | number, lines, insurance, partner, totals, payments |
| `cashbox` | `CashRegister`, `JournalEntry`, `Expense` | code, owner, movements |

### 5.2 Identifier strategy
- **Surrogate key:** ULID (`io.azam.ulidj`), 26-character lexically-sortable string stored as `varchar(26)` PK. Generated by the application before persistence to keep IDs stable across the full lifetime of the entity.
- **Business key:** every aggregate has one (`patient.labCode`, `analysis.code`, `labFile.registrationNumber`). These are unique, indexed, and immutable.

### 5.3 Value Objects
Modeled as Java records:

```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        if (amount.scale() > currency.getDefaultFractionDigits()) {
            amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        }
    }
    public Money add(Money other) { /* same-currency check */ }
}

public record ReferenceRange(BigDecimal min, BigDecimal max, String unit) { }

public record DailyId(LocalDate day, int sequence) {
    public String format(DisciplinePrefix prefix) {
        return "%d%02d%02d%02d-%03d".formatted(
            prefix.code(), day.getDayOfMonth(),
            day.getMonthValue(), day.getYear() % 100, sequence);
    }
}
```

### 5.4 Domain Events
Examples:
- `LabFileCreated`, `SampleCollected`, `ResultsEntered`, `BiologicalValidationDone`, `ResultPublished`
- `InvoiceIssued`, `PaymentReceived`
- `PatientMerged`

Events are published with Spring's `ApplicationEventPublisher`. Subscribers in other modules listen via `@TransactionalEventListener(phase = AFTER_COMMIT)` to keep boundaries clean.

### 5.5 Sealed Types for Result Values
```java
public sealed interface ResultValue
    permits NumericResult, IntegerResult, TextResult, FormulaResult, FreeFormResult {

    String renderForReport();
}
```
Pattern matching on `ResultValue` keeps result handling exhaustive at compile time.

---

## 6. Database Design

> *Note: the code samples in this section and Appendix A still show `UUID` columns. They migrate to `varchar(26)` ULID columns in Phase 0 scaffolding; the §5.2 decision above is the binding contract.*

### 6.1 Conventions
- Schema: `lims`
- Table names: `snake_case`, plural (`patients`, `lab_files`)
- Every table has: `id UUID PK`, `created_at`, `updated_at`, `created_by`, `updated_by`, `version` (JPA optimistic lock)
- Soft delete via `deleted_at` only where the spec explicitly demands historical recovery (lab files, results); hard delete otherwise
- Money columns: `NUMERIC(18,2)` and `currency_code CHAR(3)`
- All FKs have an index

### 6.2 Core Tables (excerpt)

```sql
CREATE TABLE patients (
    id UUID PRIMARY KEY,
    lab_code VARCHAR(20) NOT NULL UNIQUE,
    last_name VARCHAR(120) NOT NULL,
    first_names VARCHAR(160),
    maiden_name VARCHAR(120),
    civility VARCHAR(20) NOT NULL,
    sex CHAR(1) NOT NULL,
    birth_date DATE,
    id_document_type VARCHAR(20),
    id_document_number VARCHAR(40),
    profession VARCHAR(80),
    address VARCHAR(255),
    phone VARCHAR(30),
    email VARCHAR(120),
    insured BOOLEAN NOT NULL DEFAULT FALSE,
    insurance_id UUID REFERENCES insurances(id),
    partner_id UUID REFERENCES partners(id),
    portal_login VARCHAR(80),
    portal_password_hash VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_patients_last_name ON patients (lower(last_name));
CREATE INDEX idx_patients_phone     ON patients (phone);
```

```sql
CREATE TABLE lab_files (
    id UUID PRIMARY KEY,
    registration_number VARCHAR(30) NOT NULL UNIQUE,
    daily_id VARCHAR(20) NOT NULL,
    patient_id UUID NOT NULL REFERENCES patients(id),
    prescriber_id UUID REFERENCES prescribers(id),
    is_urgent BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,        -- DRAFT, IN_PROGRESS, VALIDATED, PRINTED, SENT, CANCELLED
    appointment_at TIMESTAMPTZ,
    diagnosis TEXT,
    total_amount NUMERIC(18,2) NOT NULL DEFAULT 0,
    insurance_coverage NUMERIC(18,2) NOT NULL DEFAULT 0,
    patient_due NUMERIC(18,2) NOT NULL DEFAULT 0,
    discount NUMERIC(18,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
```

### 6.3 Reference Intervals
Reference intervals are **per analysis × patient category × age range × sex**, with multiple modes (Standard, Comment, None). Modeled with a single table and a `mode` discriminator:

```sql
CREATE TABLE reference_intervals (
    id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL REFERENCES analyses(id),
    category VARCHAR(20) NOT NULL,    -- MAN, WOMAN, CHILD, NEWBORN
    sex CHAR(1),
    age_min INT, age_max INT, age_unit VARCHAR(10),
    mode VARCHAR(15) NOT NULL,        -- STANDARD, COMMENT, NONE
    unit_1_min NUMERIC(18,4), unit_1_max NUMERIC(18,4),
    unit_2_min NUMERIC(18,4), unit_2_max NUMERIC(18,4),
    comment_text TEXT
);
```

### 6.4 Antibiogram
```sql
CREATE TABLE antibiotics (
    id UUID PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    family VARCHAR(50),
    reference_diameter NUMERIC(5,2) NOT NULL,
    intermediate_lower NUMERIC(5,2),
    intermediate_upper NUMERIC(5,2)
);
```
Interpretation logic:
- `m < lower` → **R**
- `m > upper` → **S**
- `lower ≤ m ≤ upper` → **I**

### 6.5 Migrations (Liquibase)

All schema and reference-data changes are managed by **Liquibase**. Spring Boot picks the changelog up automatically when `spring.liquibase.change-log` is set; no manual command is needed at boot.

**Layout**

```
src/main/resources/db/changelog/
├── db.changelog-master.yaml
├── changes/
│   ├── 001-initial-schema.yaml
│   ├── 002-patient-merge-fields.yaml
│   ├── 003-reference-intervals.yaml
│   ├── 004-antibiogram.yaml
│   └── ...
└── data/
    ├── seed-disciplines.yaml
    └── seed-roles.yaml
```

**Master changelog**

```yaml
databaseChangeLog:
  - includeAll:
      path: changes/
      relativeToChangelogFile: true
  - includeAll:
      path: data/
      relativeToChangelogFile: true
      context: dev,test
```

**Changeset conventions**

- One file per logical change. Never edit a changeset that has already been applied to a shared environment — add a new one.
- `id` follows `<yyyyMMddHHmm>-<short-name>`, `author` is the developer's git handle.
- Every changeset declares a `rollback` block (or explicitly states `<empty/>` rollback when impossible) so non-prod environments can step backward.
- `contexts` separate environment-specific data: `dev`, `test`, `staging`, `prod`. Seed data lives only in `dev,test` contexts.
- `labels` mark cross-cutting changes for filtered execution (e.g. `label="phase-2"`).
- `preConditions` guard idempotency on environments that may already contain data (e.g. `onFail: MARK_RAN` for additive changes).

**Example changeset**

```yaml
databaseChangeLog:
  - changeSet:
      id: 202604030900-create-patients
      author: samuel
      labels: phase-1
      changes:
        - createTable:
            tableName: patients
            columns:
              - column:
                  name: id
                  type: uuid
                  constraints: { primaryKey: true, nullable: false }
              - column:
                  name: lab_code
                  type: varchar(20)
                  constraints: { nullable: false, unique: true }
              - column:
                  name: last_name
                  type: varchar(120)
                  constraints: { nullable: false }
              - column:
                  name: first_names
                  type: varchar(160)
              - column:
                  name: civility
                  type: varchar(20)
                  constraints: { nullable: false }
              - column:
                  name: sex
                  type: char(1)
                  constraints: { nullable: false }
              - column:
                  name: created_at
                  type: timestamptz
                  defaultValueComputed: now()
                  constraints: { nullable: false }
              - column:
                  name: updated_at
                  type: timestamptz
                  defaultValueComputed: now()
                  constraints: { nullable: false }
              - column:
                  name: version
                  type: int
                  defaultValueNumeric: 0
                  constraints: { nullable: false }
        - createIndex:
            indexName: idx_patients_last_name
            tableName: patients
            columns:
              - column: { name: last_name, computed: true, descending: false }
      rollback:
        - dropTable:
            tableName: patients
```

**Operational rules**

- `mvn liquibase:status` runs in CI on every pull request and fails the build if a changeset has been edited after being applied to staging.
- `mvn liquibase:updateSQL` produces a script attached to the deployment artifact for the DBA to review before prod runs.
- `mvn liquibase:rollbackOneUpdateSQL` is generated alongside, so a rollback path is reviewed before a release goes live.
- A nightly `liquibase:diff` job compares the JPA-derived schema against the running database and alerts on drift.
- A small smoke test in CI runs `update` then `rollback` to the previous tag against an empty PostgreSQL container, ensuring every changeset has a working reverse path.

---

## 7. REST API Design

### 7.1 Conventions
- Base path: `/api/v1`
- JSON only (`application/json`)
- Cursor-based pagination for large lists; offset for admin lists
- Filtering: `?status=VALIDATED&from=2026-04-01&to=2026-04-30`
- Sorting: `?sort=createdAt,desc`
- All write endpoints require `Idempotency-Key` header for safe retries

### 7.2 Standard Error Body (RFC 7807)
```json
{
  "type": "https://lims/errors/validation",
  "title": "Validation failed",
  "status": 422,
  "detail": "1 field is invalid",
  "instance": "/api/v1/patients",
  "errors": [
    { "field": "email", "code": "Email", "message": "must be a valid email" }
  ],
  "traceId": "3f1a..."
}
```

### 7.3 Endpoint Catalog (excerpt)

#### Patients
| Method | Path | Notes |
|---|---|---|
| `POST` | `/patients` | create patient, generates `labCode` |
| `GET` | `/patients/{id}` | full record |
| `GET` | `/patients` | search by name, phone, idDocument, labCode |
| `PATCH` | `/patients/{id}` | partial update |
| `POST` | `/patients/{id}/merge` | merge into another patient |

#### Lab Files
| Method | Path | Notes |
|---|---|---|
| `POST` | `/lab-files` | create file, returns `registrationNumber` |
| `GET` | `/lab-files/{id}` | full file with samples + analyses |
| `POST` | `/lab-files/{id}/samples` | add a sample collection event |
| `POST` | `/lab-files/{id}/analyses` | add analyses (codes) |
| `POST` | `/lab-files/{id}/results:enter` | enter results (technical) |
| `POST` | `/lab-files/{id}/results:validate-tech` | technical validation |
| `POST` | `/lab-files/{id}/results:validate-bio` | biological validation, locks the file |
| `POST` | `/lab-files/{id}:publish` | publish online + email |

#### Quotes
| `POST` `/quotes` · `GET` `/quotes/{id}` · `POST` `/quotes/{id}:lock` · `POST` `/quotes/{id}:send` |

#### Catalog
| `GET/POST/PUT/DELETE` `/analyses`, `/analyses/grouped`, `/disciplines`, `/matrices`, `/techniques`, `/recipients`, `/antibiotics`, `/prescribers`, `/insurances`, `/partners`, `/society-panels`, `/collectors` |

#### Billing & Cashbox
| `POST` `/invoices` (auto from a lab file) · `POST` `/payments` · `POST` `/expenses` · `GET` `/cash-registers/{id}/journal?date=...` |

#### Statistics
| `GET` `/stats?from=...&to=...&groupBy=discipline,prescriber&filters=...` |

### 7.4 Versioning
Major version in URL (`/v1`). Breaking changes ship as `/v2`; old versions stay alive for at least one release cycle.

### 7.5 OpenAPI
Every controller is annotated; the schema is generated at `/v3/api-docs` and rendered at `/swagger-ui.html`. CI fails the build if a non-trivial API change has no matching OpenAPI snapshot diff.

---

## 8. Module Implementation Details

### 8.1 `patient` Module
**Lab code generation.** A monotonic counter per laboratory keeps generation cheap and predictable. The format is `<labPrefix>-<yyMMdd>-<seq>`. Generation runs inside the same transaction as patient creation, so failures roll back the counter.

**Patient merge.** Source records remain in the database with `merged_into_id` set; all foreign keys are rewritten to the surviving record inside one transaction. A `PatientMerged` event is published so attachments, invoices, and stats projections move accordingly.

### 8.2 `catalog` Module
**Simple vs. grouped analyses.** A single `analysis` table with a `kind` column (`SIMPLE` or `GROUPED`). Grouped analyses link to children through `analysis_components(parent_id, child_id, position)`. The duplication seen in the original spec is eliminated by sharing all configuration tabs (Conservation, Techniques, Units, Delay, Reference intervals) at the parent level.

**Reference interval editor.** When a user fills the first category, the API offers a `POST /analyses/{id}/intervals:replicate` call to copy the data into the other categories.

**Antibiogram interpretation** is a pure function on the domain side, so it stays independent of HTTP and is unit-tested in isolation.

### 8.3 `order` Module
**Daily ID.** Generated atomically via `SELECT ... FOR UPDATE` on a `daily_counters(day, discipline_prefix)` row, falling back to `nextval` on a per-discipline sequence. Format follows the spec: `<prefix><dd><MM><yy>-<seq>`.

**Sample collection.** Three sample sub-types (`IN_LAB`, `HOME`, `EXTERNAL_RECEIVED`) modeled with a sealed hierarchy persisted as single-table inheritance with a discriminator. Validation rules differ per type (a home collection needs a distance and time window; an external sample needs a receiving operator).

**Urgency flag** is a boolean on the file, but the module also raises `LabFileMarkedUrgent` so the dashboard can highlight it without polling.

### 8.4 `result` Module
**Two-step validation.** A result becomes available for biological validation only after technical validation. The API enforces ordering with state-machine guards. The biological validation event triggers invoice finalization, label printing, and result delivery.

**Out-of-range detection** runs on save by comparing the stored `ResultValue` to the matched `ReferenceRange` for the patient's category/age/sex. The `is_out_of_range` flag is denormalized for fast filtering.

**File attachments.** Scanned PDFs are uploaded to object storage (S3-compatible: MinIO or AWS S3); only the URI is kept in the database.

### 8.5 `billing` Module
**Invoice number** format follows the spec: `<labCode>-<yyMMdd>-<seq>` (e.g. `LTA24-1710-005`). Generated per lab via a per-day counter table.

**Insurance pricing.** Insurance coverage is computed on the canonical price (`B-value × B-rate`) multiplied by the coverage rate. Discounts are applied last and can be a fixed amount or a percentage.

**Society panels** are modeled as a special `Analysis` with `kind = SOCIETY_PANEL` and its own indiciary value, billed as a single line.

### 8.6 `cashbox` Module
A cash register belongs to one or more receptionist accounts. Each payment posts a journal entry; an end-of-day close-out aggregates totals per payment mode (cash, cheque, TPE, Flooz, Mixx by Yas). Expenses post into the same journal with a negative sign and require a justification scan.

### 8.7 `delivery` Module
Three channels: print, email, online portal.
- **Print** generates the PDF report on demand and streams it back; a copy is archived in object storage.
- **Email** sends the PDF as an attachment, password-protected with a per-file random password delivered via SMS or WhatsApp.
- **Online** publishes the PDF behind a per-file token. The patient logs in with the credentials issued at registration.

### 8.8 `stats` Module
Heavy aggregates are precomputed nightly into materialized views (`mv_daily_files`, `mv_results_by_discipline`). Live filtering on top stays fast. Filters supported match the spec (prescribers, disciplines, analyses, normal/abnormal, antibiograms, insurances, partners, new/old patients, sex, age groups).

---

## 9. Security & Authentication

### 9.1 Authentication
- **Local auth** with username + password (Argon2id hashing, 19 MB memory, 2 iterations, 1 thread)
- **JWT** access tokens (15 min) + refresh tokens (8 h, rotating)
- **MFA** (TOTP, RFC 6238) recommended for biologists and admins
- Brute-force protection: progressive lockout after failed attempts, IP throttling via Bucket4j

### 9.2 Authorization
Role-based with fine-grained permissions:

| Role | Sample permissions |
|---|---|
| `RECEPTIONIST` | patient.create, lab-file.create, payment.collect |
| `COLLECTOR` | sample.record |
| `TECHNICIAN` | result.enter, result.validate-tech |
| `BIOLOGIST` | result.validate-bio, file.publish |
| `CASHIER` | invoice.read, payment.collect, expense.record |
| `ACCOUNTANT` | invoice.*, expense.*, stats.read |
| `LAB_MANAGER` | * (configuration), users.manage |
| `ADMIN` | system, audit, backup |

Method-level enforcement via `@PreAuthorize("hasAuthority('result.validate-bio')")`.

### 9.3 Data Protection
- TLS 1.3 only, HSTS enabled
- Secrets in environment / Vault, never in code
- PII columns encrypted at the application layer where regulation demands (id document numbers, portal passwords)
- Database backups encrypted at rest
- Audit log for every read of a patient file by a user (separate `audit_access` table)

### 9.4 Audit Trail
Two layers:
- **Hibernate Envers** maintains row-level history on all critical tables (`patients`, `lab_files`, `result_entries`, `invoices`, `users`)
- **Application events** logged into an append-only `audit_event` table (who, what, when, target, ip, userAgent)

---

## 10. Cross-Cutting Concerns

### 10.1 Validation
- Bean Validation on every DTO entering the controller layer
- Domain invariants enforced inside aggregates (compact constructors on records, factory methods on entities)
- A custom `@LabCode` annotation validates business identifiers

### 10.2 Exception Handling
Global `@ControllerAdvice` converts:
- `MethodArgumentNotValidException` → 422 with a list of field errors
- `EntityNotFoundException` → 404
- `OptimisticLockException` → 409 with retry hint
- `AccessDeniedException` → 403
- Uncaught exceptions → 500 with a `traceId`; the stack trace is logged but never returned

### 10.3 Transactions
- Service methods are `@Transactional` by default
- Read endpoints use `@Transactional(readOnly = true)`
- Long-running operations (bulk export, statistics) run outside a transaction, paginating cursors

### 10.4 Concurrency
- Optimistic locking via JPA `@Version` everywhere
- Pessimistic locks only for the daily-counter rows
- Virtual threads enabled via `spring.threads.virtual.enabled=true` for the Tomcat executor

### 10.5 Internationalization
All user-facing messages go through `MessageSource`. Default locale `fr-FR`; `en-US` available. Reference data (analyses, disciplines) is stored in the user's working language and not auto-translated.

---

## 11. File & Document Handling

### 11.1 Storage
- **Object store** (MinIO on-premise; S3-compatible) for: scanned attachments, generated PDFs (invoices, lab reports), expense receipts
- **Database** holds only the URI, content type, size, and SHA-256 hash
- Scans uploaded via multipart; max size 25 MB, type-checked by magic bytes (Apache Tika)

### 11.2 PDF Reports
- Templates live in `templates/reports/*.html` and are rendered with **Thymeleaf** then converted to PDF with **OpenHTMLtoPDF**
- A header layout, a centered patient block, the result table with out-of-range bolding, footnotes, biologist signature, and a barcode for the daily ID

### 11.3 Labels
Label printing is delegated to the frontend or a dedicated print service. The backend exposes `GET /lab-files/{id}/labels` which returns label records (patient name, dob, age, sex, daily ID barcode, first 5 analyses) — the printer driver renders them to 4 cm × 2.5 cm thermal labels.

### 11.4 Network Scanner Discovery
Out of scope for the backend. The frontend is responsible for SANE/TWAIN discovery; it uploads the resulting image/PDF via `POST /attachments`.

---

## 12. Notifications (Mail / WhatsApp / SMS)

### 12.1 Architecture
A `notification` module exposes a single API:
```java
public interface NotificationService {
    void send(NotificationRequest request);
}
public sealed interface NotificationRequest
    permits EmailRequest, WhatsAppRequest, SmsRequest { }
```
Implementations behind the same interface plug in different providers.

### 12.2 Providers
- **Email** — SMTP (Spring Mail) or SendGrid / Amazon SES
- **WhatsApp** — Meta Cloud API (templated messages for transactional content)
- **SMS** — local gateway (Orange/Moov/MTN) or Twilio

### 12.3 Reliability
All sends go through an outbox table. A scheduled job retries failures with exponential backoff. The audit log records every successful or failed delivery against the lab file.

---

## 13. Reporting & Statistics

### 13.1 Online Aggregates
Endpoints under `/stats` accept a date range and a list of grouping dimensions plus filters. Implemented with JPA Criteria + native SQL when needed for performance.

### 13.2 Materialized Views
Refreshed nightly:
- `mv_daily_kpis(date, lab_files_count, validated_count, revenue_total, urgent_count)`
- `mv_results_by_discipline(date, discipline_id, count, abnormal_count)`
- `mv_antibiogram_summary(date, antibiotic_id, sensitive, intermediate, resistant)`

### 13.3 Exports
XLSX (Apache POI) and PDF (JasperReports) for accountants and the lab manager.

---

## 14. Integrations

### 14.1 Lab Instruments (planned, phase 2)
HL7 v2 / ASTM E1394 over TCP. A dedicated `instruments` module will host MLLP listeners and parse messages into `ResultEntry` objects. Until then, results are entered manually.

### 14.2 Payment Gateways
- **Flooz** (Moov Africa) and **Mixx by Yas** (Yas Togo) — integration via their merchant APIs once the lab has API credentials. Until then, payments are recorded manually with a transaction reference field.

### 14.3 Insurance APIs
Most West African insurances do not yet expose APIs; pricing is configured locally. The architecture accepts a future `InsuranceClient` adapter without disturbing billing.

---

## 15. Testing Strategy

The whole codebase is delivered through **Test-Driven Development**. Every behavior — domain rule, controller mapping, persistence query, security guard — exists because a failing test asked for it. No production code is written without a red test first; no test stays green without the smallest implementation that satisfies it; refactoring happens once both tests and production code are clean.

### 15.1 The TDD Loop

The team follows the canonical **Red → Green → Refactor** cycle, applied at three nested scopes (outside-in):

1. **Acceptance test (red)** — start the slice with a failing high-level test that captures the user-visible behavior (e.g. "creating a lab file returns 201 with a registration number and persists a row"). This stays red until the whole slice is done.
2. **Unit test (red)** — drop into the domain or service layer and write a smaller failing test for the specific collaborator the acceptance test is missing.
3. **Implementation (green)** — write the minimum code to make the unit test pass. No speculative methods, no premature abstractions.
4. **Refactor** — with both tests and code green, clean up duplication, naming, and structure. Run all tests after each rename.
5. **Climb back up** — once enough unit tests are green, the acceptance test should also pass; if not, write the next failing unit test.

A commit is forbidden if any test is red or skipped. CI rejects PRs whose tests were added *after* the production code in the same commit (we enforce this with a lightweight check on the diff: every PR must include test changes touching the same module as production changes).

### 15.2 Test Pyramid

The pyramid below is what the team writes; the percentages are rough targets, not quotas.

| Level | Tooling | Speed | Share | Purpose |
|---|---|---|---|---|
| Unit (domain) | JUnit 5, AssertJ, Mockito | < 50 ms each | ~70% | Pure business rules: reference intervals, antibiogram math, invoice computation, daily-ID format, sample-state transitions |
| Slice (web / persistence) | `@WebMvcTest`, `@DataJpaTest`, MockMvc, Spring Security Test | < 500 ms | ~20% | Controller routing, validation, JSON shape, JPA query correctness |
| Integration (module) | `@SpringBootTest` + Testcontainers (PostgreSQL, MinIO) | seconds | ~8% | One end-to-end happy path and key error paths per module, hitting the real DB |
| Architecture | ArchUnit + Spring Modulith `ApplicationModules.verify()` | seconds | ~1% | Module boundaries, layer dependencies, naming rules |
| Acceptance / E2E | RestAssured against a running instance | seconds | ~1% | Cross-module flows: register patient → create file → enter results → validate → publish → invoice |

### 15.3 Outside-In Example: "Create a lab file"

**Step 1 — failing acceptance test**

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class CreateLabFileAcceptanceTest {

    @Test
    void creates_lab_file_and_returns_registration_number() {
        var patient = given.aPatient();

        var response = api.post("/api/v1/lab-files", new CreateLabFileRequest(
                patient.id(), prescriberId, false, "fasting"));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body().path("registrationNumber")).isNotBlank();
        assertThat(db.countLabFiles()).isEqualTo(1);
    }
}
```
Fails: no controller, no service, no entity. Good.

**Step 2 — drop into the unit layer**

```java
class DailyIdGeneratorTest {
    @Test
    void increments_per_day_and_discipline() {
        var gen = new DailyIdGenerator(new InMemoryCounter());
        assertThat(gen.next(BIOCHEMISTRY).format()).isEqualTo("103052026-001");
        assertThat(gen.next(BIOCHEMISTRY).format()).isEqualTo("103052026-002");
        assertThat(gen.next(HEMATOLOGY).format()).isEqualTo("203052026-001");
    }
}
```
Write the smallest `DailyIdGenerator` that makes these three assertions pass.

**Step 3 — climb back up**, write the next failing unit test (`LabFileFactoryTest`), then `LabFileServiceTest` (with mocks), then the controller `@WebMvcTest`. The acceptance test goes green only when all collaborators are real.

### 15.4 Test Doubles Policy

- **Mocks** (Mockito) only at the *seam* between layers (service mocks the repository in a unit test). Do not mock value objects or pure domain functions.
- **In-memory fakes** preferred over mocks for repositories in domain unit tests (`InMemoryLabFileRepository implements LabFileRepository`). They make tests read like specifications.
- **Stubs** for outbound adapters (mail provider, WhatsApp). The real adapter has its own integration test against the provider's sandbox.
- Never mock `Clock`, `UUIDProvider`, or `Counter` directly — inject deterministic test implementations.

### 15.5 Test Data

- A `TestDataBuilder` per aggregate (`PatientBuilder.aPatient().withSex(F).build()`) — readable, default-safe, override-explicit.
- Seed data via Liquibase changelogs scoped to the `test` context (`db/changelog/data/*.yaml`).
- Datafaker for bulk realistic data in performance tests.
- A small `LimsScenarios` helper composes builders into common situations (`scenarios.aValidatedFile()`).

### 15.6 Coverage Targets

Coverage is a *floor*, not a goal. Targets:

| Area | Line | Branch |
|---|---|---|
| Value objects | 100% | 100% |
| Domain services (billing, intervals, antibiogram, daily-ID) | 95% | 90% |
| Application services | 90% | 85% |
| Web layer | 85% | — |
| Overall | 80% | 75% |

**Mutation testing** with PIT runs nightly on the domain packages. A killed-mutation rate below 75% breaks the nightly build.

### 15.7 Definition of Done (per change)

A change is considered done when:

- The failing test that motivated it is green.
- All other tests are green (unit, slice, integration, architecture).
- The slice has at least one happy-path acceptance test and one error-path test.
- Coverage thresholds are met.
- Mutation score on touched files has not dropped.
- Liquibase rollback is included for any new changeset.
- The PR description names the test file that *first failed* and now passes.

### 15.8 Performance Testing

- **Gatling** scenarios for the most-hit endpoints (patient search, lab-file detail, result entry).
- Run quarterly, on schema changes, and before any production release that includes a new index, materialized view, or query.
- Performance budgets are themselves expressed as tests: a Gatling assertion fails the build if p95 latency or error rate crosses the threshold.

---

## 16. Observability

### 16.1 Metrics (Micrometer → Prometheus)
- HTTP latency histograms by endpoint
- Database connection pool usage
- Domain counters: `lab_files_created_total`, `results_validated_total`, `invoices_issued_total`
- Outbox queue depth

### 16.2 Logs
- JSON to stdout
- Correlation: `traceId`, `spanId`, `userId`, `labFileId`
- No PII in log bodies — patient names are replaced with their UUID at the controller layer

### 16.3 Tracing
OpenTelemetry exporter to a collector (Jaeger or Tempo). Every controller call carries a trace ID echoed back in the `X-Trace-Id` response header.

### 16.4 Alerts
- API error rate > 1% over 5 min
- p95 latency > 500 ms over 10 min
- Outbox lag > 100 messages
- Disk free < 20% on the database server
- No backup taken in the last 26 hours

---

## 17. Deployment & DevOps

### 17.1 Build
- Maven multi-module is overkill at this scope; one Maven project with Spring Modulith is enough
- `mvn -P prod package` builds an executable jar
- Container image built with Cloud Native Buildpacks (`mvn spring-boot:build-image`) using the `paketobuildpacks/builder:base` and `BP_JVM_VERSION=25`

### 17.2 Environments
| Env | Purpose | Data |
|---|---|---|
| `local` | dev laptops | seeded fake data |
| `dev` | integration | fake data, reset nightly |
| `staging` | pre-prod | masked subset of prod |
| `prod` | live | real |

### 17.3 Deployment Topology (single lab)
- 1 application container (8 GB RAM, 2 vCPU)
- 1 PostgreSQL 17 instance (16 GB RAM, SSD, 100 GB)
- 1 MinIO bucket
- 1 reverse proxy (Caddy or Nginx) with Let's Encrypt
- 1 daily backup job (`pg_dump` + S3 archival) and continuous WAL archiving

### 17.4 CI/CD with Jenkins

The pipeline runs on a **Jenkins** controller (LTS) with at least two agents labelled `linux-jdk25` (Maven, Docker, Java 25). Builds are declarative pipelines stored in the repository as a `Jenkinsfile`. Every pull request and every push to `main` triggers the same pipeline; release tags trigger the production deploy stage.

**Pipeline stages**

1. **Checkout** — full clone with tags so Liquibase status checks see history.
2. **Format & lint** — Spotless and Checkstyle; failures block the build.
3. **Compile** — `mvn -B compile`.
4. **Unit tests** — fast feedback loop; runs the unit pyramid layer only. Builds fail if any test fails or is skipped.
5. **Slice tests** — `@WebMvcTest` and `@DataJpaTest` slices.
6. **Integration tests** — Testcontainers spins up PostgreSQL + MinIO; `mvn -B verify -Pintegration`.
7. **Architecture tests** — ArchUnit + Spring Modulith verifier.
8. **Mutation tests** — PIT on touched modules (PR builds) or all modules (nightly). Threshold: 75% killed.
9. **Liquibase verification** — `mvn liquibase:status`, `liquibase:updateSQL`, and a smoke `update → rollback → update` against an ephemeral DB.
10. **Coverage gate** — JaCoCo report; fails below thresholds in §15.6.
11. **Security scans** — OWASP Dependency-Check, Trivy on the built image.
12. **Package** — `mvn -B spring-boot:build-image` produces a labelled OCI image with SBOM.
13. **Publish** — push image to the internal registry on `main`; tag as `latest` and as the git short SHA.
14. **Deploy to staging** — automated on `main`. Includes a Liquibase `update` against the staging DB before the new app version starts.
15. **Deploy to prod** — manual `input` step; only enabled for tagged releases. Production deploys run a *blue-green* swap behind the reverse proxy.

The pipeline aborts as soon as a stage fails; the team treats a red Jenkins build as a stop-the-line event.

**`Jenkinsfile`**

```groovy
pipeline {
    agent { label 'linux-jdk25' }

    options {
        timestamps()
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
        timeout(time: 45, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    environment {
        MAVEN_OPTS = '-Xmx2g -XX:+UseG1GC'
        REGISTRY   = 'registry.internal/lims'
        IMAGE_NAME = "${REGISTRY}/lims-backend"
        SHORT_SHA  = "${GIT_COMMIT.take(8)}"
    }

    triggers {
        // PRs and pushes via webhook; nightly mutation run at 02:00
        cron(env.BRANCH_NAME == 'main' ? 'H 2 * * *' : '')
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                sh 'git fetch --tags'
            }
        }

        stage('Format & Lint') {
            steps {
                sh 'mvn -B -ntp spotless:check checkstyle:check'
            }
        }

        stage('Compile') {
            steps {
                sh 'mvn -B -ntp -DskipTests compile'
            }
        }

        stage('Unit Tests (TDD inner loop)') {
            steps {
                sh 'mvn -B -ntp test -Punit'
            }
            post {
                always { junit 'target/surefire-reports/*.xml' }
            }
        }

        stage('Slice Tests') {
            steps {
                sh 'mvn -B -ntp test -Pslice'
            }
            post {
                always { junit 'target/surefire-reports/*.xml' }
            }
        }

        stage('Integration Tests') {
            steps {
                sh 'mvn -B -ntp verify -Pintegration -DskipUnit'
            }
            post {
                always { junit 'target/failsafe-reports/*.xml' }
            }
        }

        stage('Architecture Tests') {
            steps {
                sh 'mvn -B -ntp test -Parchitecture'
            }
        }

        stage('Mutation Tests') {
            when {
                anyOf {
                    branch 'main'
                    triggeredBy 'TimerTrigger'
                    changeRequest()
                }
            }
            steps {
                sh 'mvn -B -ntp org.pitest:pitest-maven:mutationCoverage'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/pit-reports/**/*', allowEmptyArchive: true
                }
            }
        }

        stage('Liquibase Verification') {
            steps {
                sh '''
                    mvn -B -ntp liquibase:status
                    mvn -B -ntp liquibase:updateSQL -Dliquibase.outputFile=target/liquibase-update.sql
                    mvn -B -ntp -Pliquibase-smoke verify
                '''
                archiveArtifacts artifacts: 'target/liquibase-update.sql', allowEmptyArchive: false
            }
        }

        stage('Coverage Gate') {
            steps {
                sh 'mvn -B -ntp jacoco:report jacoco:check'
                publishHTML(target: [
                    reportDir: 'target/site/jacoco',
                    reportFiles: 'index.html',
                    reportName: 'JaCoCo Coverage'
                ])
            }
        }

        stage('Security Scans') {
            parallel {
                stage('Dependency-Check') {
                    steps {
                        sh 'mvn -B -ntp org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7'
                    }
                }
                stage('Trivy (FS)') {
                    steps {
                        sh 'trivy fs --exit-code 1 --severity HIGH,CRITICAL .'
                    }
                }
            }
        }

        stage('Package Image') {
            steps {
                sh 'mvn -B -ntp -DskipTests spring-boot:build-image -Dspring-boot.build-image.imageName=${IMAGE_NAME}:${SHORT_SHA}'
                sh 'syft ${IMAGE_NAME}:${SHORT_SHA} -o spdx-json > target/sbom.spdx.json'
                archiveArtifacts artifacts: 'target/sbom.spdx.json'
            }
        }

        stage('Trivy (Image)') {
            steps {
                sh 'trivy image --exit-code 1 --severity HIGH,CRITICAL ${IMAGE_NAME}:${SHORT_SHA}'
            }
        }

        stage('Publish Image') {
            when { anyOf { branch 'main'; buildingTag() } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'registry-creds',
                                                  usernameVariable: 'REG_USER',
                                                  passwordVariable: 'REG_PASS')]) {
                    sh '''
                        echo "$REG_PASS" | docker login $REGISTRY -u "$REG_USER" --password-stdin
                        docker push ${IMAGE_NAME}:${SHORT_SHA}
                        if [ "${BRANCH_NAME}" = "main" ]; then
                            docker tag  ${IMAGE_NAME}:${SHORT_SHA} ${IMAGE_NAME}:latest
                            docker push ${IMAGE_NAME}:latest
                        fi
                    '''
                }
            }
        }

        stage('Deploy: Staging') {
            when { branch 'main' }
            steps {
                withCredentials([file(credentialsId: 'staging-kubeconfig', variable: 'KUBECONFIG')]) {
                    sh '''
                        mvn -B -ntp liquibase:update -Pstaging
                        kubectl --kubeconfig=$KUBECONFIG \
                            set image deployment/lims-backend \
                            lims-backend=${IMAGE_NAME}:${SHORT_SHA} -n lims-staging
                        kubectl --kubeconfig=$KUBECONFIG \
                            rollout status deployment/lims-backend -n lims-staging --timeout=5m
                    '''
                }
            }
        }

        stage('Deploy: Production') {
            when { buildingTag() }
            steps {
                input message: "Deploy ${TAG_NAME} to PRODUCTION?", ok: 'Deploy', submitter: 'lab-manager,ops'
                withCredentials([file(credentialsId: 'prod-kubeconfig', variable: 'KUBECONFIG')]) {
                    sh '''
                        mvn -B -ntp liquibase:updateSQL -Pprod -Dliquibase.outputFile=target/prod-update.sql
                    '''
                    archiveArtifacts artifacts: 'target/prod-update.sql'
                    input message: "Reviewed prod-update.sql? Proceed with liquibase update + blue-green swap?", ok: 'Yes'
                    sh '''
                        mvn -B -ntp liquibase:update -Pprod
                        kubectl --kubeconfig=$KUBECONFIG \
                            set image deployment/lims-backend-green \
                            lims-backend=${IMAGE_NAME}:${SHORT_SHA} -n lims-prod
                        kubectl --kubeconfig=$KUBECONFIG \
                            rollout status deployment/lims-backend-green -n lims-prod --timeout=5m
                        kubectl --kubeconfig=$KUBECONFIG \
                            patch service lims-backend -n lims-prod \
                            -p '{"spec":{"selector":{"slot":"green"}}}'
                    '''
                }
            }
        }
    }

    post {
        success {
            slackSend channel: '#lims-ci', color: 'good',
                      message: ":white_check_mark: ${env.JOB_NAME} #${env.BUILD_NUMBER} (${SHORT_SHA}) passed."
        }
        failure {
            slackSend channel: '#lims-ci', color: 'danger',
                      message: ":x: ${env.JOB_NAME} #${env.BUILD_NUMBER} failed at stage ${env.STAGE_NAME}."
        }
        always {
            cleanWs(patterns: [[pattern: 'target/**', type: 'INCLUDE']])
        }
    }
}
```

**Required Jenkins plugins**

`Pipeline`, `Pipeline: Stage View`, `Pipeline Utility Steps`, `Git`, `JUnit`, `JaCoCo`, `HTML Publisher`, `Credentials Binding`, `Slack Notification`, `AnsiColor`, `Timestamper`, `Docker Pipeline`, `Kubernetes CLI`, `OWASP Dependency-Check`.

**Branching & releases**

- Trunk-based development on `main`.
- PRs run the full pipeline minus deploy stages.
- Merging to `main` triggers a staging deploy and a Jenkins-tagged build (`v<YYYY>.<0M>.<BUILD_NUMBER>`, e.g. `v2026.05.142`).
- The production deploy is a separate stage gated by a manual approval; the approval authorizes the *ship*, not the act of tagging.
- Hotfixes branch from a release tag, fix forward, and produce a new monotonically-numbered tag through the same pipeline.

### 17.5 Configuration
- 12-factor: every environment-specific value comes from env vars
- Secrets in HashiCorp Vault (or simply Docker secrets for small deployments)
- Configuration validated at boot with `@ConfigurationProperties` + Bean Validation

---

## 18. Performance & Scalability

| Concern | Approach |
|---|---|
| Hot patient search | trigram index `gin (last_name gin_trgm_ops)` |
| Statistics queries | materialized views refreshed nightly |
| File listing | cursor pagination (`WHERE created_at < :cursor ORDER BY created_at DESC LIMIT 50`) |
| Concurrent daily-ID generation | per-discipline sequences + per-day counter table |
| PDF generation latency | async job + polling endpoint for files >5 pages |
| Long writes | batched JDBC for bulk imports |
| Read scaling (phase 2) | PostgreSQL read replica for `stats` |

---

## 19. Security Hardening Checklist

- [ ] Default deny on all endpoints; explicit `permitAll` for `/auth/*` and Actuator health
- [ ] Argon2id password hashing with per-environment cost
- [ ] CSRF disabled (stateless JWT) but CORS strictly configured per origin
- [ ] Rate limiting on `/auth/*` and `/patients/search`
- [ ] OWASP Dependency-Check in CI; fail on CVSS ≥ 7
- [ ] Container image scanned (Trivy)
- [ ] No SQL injection: 100% parameterized queries, no string concatenation
- [ ] Output PDF passwords delivered out-of-band (SMS/WhatsApp), never in the same email
- [ ] Audit log immutable: append-only, weekly hash chain
- [ ] Quarterly access review (who has which role)
- [ ] Annual penetration test
- [ ] GDPR-style right to be forgotten only after retention period (10 years for medical records)

---

## 20. Implementation Roadmap

Every phase is delivered TDD-first: each module starts from an empty Liquibase changelog and a failing acceptance test. No phase is marked done until all tests are green, mutation thresholds hold, and the Jenkins pipeline ships the build to staging.

### Phase 0 — Walking Skeleton (1 week)
- Empty Spring Boot app with one `/health` endpoint, one Liquibase changelog, one Testcontainers integration test, one Jenkinsfile stage end-to-end.
- Goal: prove the deploy pipeline before writing business code.

### Phase 1 — Core (8–10 weeks)
- `iam`, `patient`, `catalog` (analyses simple/grouped, disciplines, matrices, techniques, recipients), `order` (lab files, samples), `result` (entry, technical + biological validation, reference intervals).
- Auth (local + JWT), audit, basic PDF report.
- Deployable to a real lab for pilot with manual billing.

### Phase 2 — Billing & Cash (4–6 weeks)
- `billing` (invoices, insurance, partners, society panels).
- `cashbox` (registers, journal, expenses, home-collection fees).
- Quotes module.
- `delivery` (print + email + online portal).

### Phase 3 — Analytics & Notifications (3–4 weeks)
- `stats` with materialized views and exports.
- `notification` (email + WhatsApp + SMS) with outbox.
- Antibiogram automation polish.

### Phase 4 — Advanced & Integrations (open-ended)
- Lab instrument integration (HL7/ASTM).
- Mobile money APIs.
- Multi-lab tenant mode.
- Microservice extraction if/when scale demands it.

---

## Appendix A — Sample Code Skeletons

### A.1 Aggregate (Patient)
```java
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "lab_code", nullable = false, unique = true)
    private String labCode;

    @Embedded
    private PersonName name;

    @Enumerated(EnumType.STRING)
    private Civility civility;

    @Enumerated(EnumType.STRING)
    private Sex sex;

    @Embedded
    private ContactInfo contact;

    @Version
    private int version;

    protected Patient() { }   // JPA

    public static Patient create(NewPatient cmd, LabCode generated) {
        var p = new Patient();
        p.id = UUIDv7.generate();
        p.labCode = generated.value();
        p.name = cmd.name();
        p.civility = cmd.civility();
        p.sex = cmd.sex();
        p.contact = cmd.contact();
        return p;
    }

    public void updateContact(ContactInfo newContact) {
        this.contact = Objects.requireNonNull(newContact);
    }
}
```

### A.2 Use-case service (LabFileService)
```java
@Service
@Transactional
@RequiredArgsConstructor
public class LabFileService {

    private final LabFileRepository files;
    private final PatientApi patients;
    private final DailyIdGenerator dailyIds;
    private final ApplicationEventPublisher events;

    public LabFileId create(CreateLabFileCommand cmd, AuthenticatedUser user) {
        var patient = patients.requireById(cmd.patientId());
        var dailyId = dailyIds.next(cmd.disciplinePrefix());

        var file = LabFile.create(patient, dailyId, cmd, user);
        files.save(file);

        events.publishEvent(new LabFileCreated(file.id(), patient.id(), Instant.now()));
        return file.id();
    }
}
```

### A.3 Antibiogram interpretation
```java
public enum Sensitivity { R, I, S }

public final class AntibiogramInterpreter {
    public static Sensitivity interpret(BigDecimal measured, Antibiotic ab) {
        var ref = ab.referenceDiameter();
        var lo  = ab.intermediateLower();
        var hi  = ab.intermediateUpper();

        if (lo != null && hi != null && measured.compareTo(lo) >= 0 && measured.compareTo(hi) <= 0) {
            return Sensitivity.I;
        }
        return measured.compareTo(ref) < 0 ? Sensitivity.R : Sensitivity.S;
    }
}
```

### A.4 OpenAPI annotation example
```java
@PostMapping("/lab-files")
@Operation(summary = "Create a lab file")
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Created",
                 content = @Content(schema = @Schema(implementation = LabFileResponse.class))),
    @ApiResponse(responseCode = "422", description = "Validation error")
})
public ResponseEntity<LabFileResponse> create(
        @Valid @RequestBody CreateLabFileRequest request,
        @AuthenticationPrincipal AuthenticatedUser user) {
    var id = service.create(request.toCommand(), user);
    return ResponseEntity.created(URI.create("/api/v1/lab-files/" + id))
                         .body(LabFileResponse.of(id));
}
```

---

## Appendix B — Glossary

| Term | Meaning |
|---|---|
| **Bilan** | A bundle of analyses sold as a package |
| **Paillasse** | Discipline / lab bench (biochimie, hématologie, immuno-sérologie, microbiologie, biologie moléculaire) |
| **Récipient** | Sample container (tube, vial, jar) |
| **Préleveur** | Sample collector (a phlebotomist) |
| **B (valeur indiciaire)** | Pricing index used by West African nomenclature |
| **Antibiogramme** | Sensitivity test of a bacterium against antibiotics |
| **Validation biologique** | Final clinical sign-off by a biologist before publishing |

---

*End of document.*
