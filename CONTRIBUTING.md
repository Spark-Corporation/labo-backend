# Contributing to LIMS

## Audience and scope of this document

This document is for human contributors — developers, reviewers, and
maintainers — working on the LIMS backend.

In scope: *how we work* — branch and commit conventions, code review and
release rules, coding conventions, the test discipline, the quality gates
that block merge, and the Definition of Done.

Out of scope: *what the system does*. The architecture, domain model,
database design, REST conventions, security model, and deployment topology
all live in `LIMS-Backend-Technical-Documentation.md` at the repo root.
That document is authoritative for behavior.

AI assistants working on this repo read a distilled, directive version of
these rules in `CLAUDE.md`. `CONTRIBUTING.md` is the source of truth that
`CLAUDE.md` derives from; when the two disagree, this document wins, and
`CLAUDE.md` is updated to match.

## How to set up your local environment

Phase 0 has not yet landed, so this section is a placeholder. The minimum
to start contributing today:

- Clone the repo:
  `git clone <url> && cd laboratory-information-management-system`.
- Configure git hooks once: `git config core.hooksPath .githooks`. The
  hooks themselves are added in the Phase 0 scaffold.
- Configure signed commits — see GitHub's docs on
  [GPG commit signature verification](https://docs.github.com/en/authentication/managing-commit-signature-verification)
  or the
  [SSH commit signing](https://docs.github.com/en/authentication/managing-commit-signature-verification/about-commit-signature-verification#ssh-commit-signature-verification)
  variant. CI rejects unsigned commits on `main`.

More detailed setup (Java 25, Maven, Docker for the local Postgres + MinIO
stack) is added when Phase 0 scaffolds the application.

## Architecture at a glance

LIMS is a **modular monolith** built with Spring Modulith. One Spring Boot
4.0.x application, one deployable jar, one database.

Modules currently planned:

`iam`, `patient`, `catalog`, `order`, `result`, `billing`, `cashbox`,
`delivery`, `stats`, `notification`, `attachment`.

Module boundary contract:

- Each module exports **only** its `api/` package; everything else
  (`domain/`, `infra/`, `web/`) is package-private.
- Cross-module **reads** go through the target module's `api/` interfaces.
- Cross-module **writes** go through Spring application events with
  `@TransactionalEventListener(phase = AFTER_COMMIT)`. Synchronous
  cross-module writes are forbidden.
- An `ApplicationModules.of(...).verify()` test enforces these boundaries;
  CI fails if a module reaches into another module's internals.

For module responsibilities, the domain model, and database design, see
`LIMS-Backend-Technical-Documentation.md` §§3–6.

## Stack

| Concern | Choice | Notes |
|---|---|---|
| Language | Java 25 | LTS |
| Framework | Spring Boot 4.0.x | **Overrides LIMS doc §2.1 (which says 3.4+)** to match BGA and unify the Spring Boot version across the author's projects |
| Persistence | Spring Data JPA / Hibernate | |
| Database | PostgreSQL **17** | **Overrides init spec §2.2 and LIMS doc §2.2** (both say 16) to match gnawo's `postgres:17-alpine`. Ratified in the Phase 0 brainstorm (2026-05-04). |
| Migrations | Liquibase, YAML changelogs | |
| Connection pool | HikariCP (Spring Boot default) | |
| API docs | springdoc-openapi | |
| Error format | RFC 7807 Problem Details | |
| Identifier strategy | **ULID** via `io.azam.ulidj`, stored as `varchar(26)` PK | **Overrides LIMS doc §5.2 (which says UUIDv7)** to match BGA |
| Logging | Logback + JSON encoder | |
| Metrics | Micrometer → Prometheus | |
| Tracing | OpenTelemetry | |
| Actuator | Spring Boot Actuator | |
| Build | Maven, parent POM, revision-style versioning (`${revision}`) | Mirrors BGA |
| Lombok | Allowed: `@Slf4j`, `@RequiredArgsConstructor`, `@Getter`, `@Setter`, `@Builder`. Forbidden: `@Data`, `@SneakyThrows` | **Broader than LIMS doc §2.3 (which only allowed `@Slf4j`)** |
| Test runtime | JUnit 5 + AssertJ + Mockito + Testcontainers | |
| Architecture tests | ArchUnit + Spring Modulith verifier | |
| Mutation testing | PIT | |
| Code style | Spotless + Checkstyle | |
| Mapping | MapStruct (when needed) | |

Three rows above deliberately override the LIMS technical doc (Spring Boot
4.0.x, Lombok scope, ULID identifiers). The rationale lives in
`docs/superpowers/specs/2026-05-03-lims-init-design.md` §2.2 and the
companion patches to the technical doc itself.

## Project naming and packaging

| Item | Value |
|---|---|
| Maven `groupId` | `org.sparkcorporation.lims` |
| Maven `artifactId` (parent / app) | `lims-backend` |
| Maven `version` | `${revision}` placeholder; Jenkins computes it at build time (see Releases) |
| Java root package | `org.sparkcorporation.lims` (modules: `org.sparkcorporation.lims.iam`, `org.sparkcorporation.lims.patient`, …) |
| Spring Boot main class | `org.sparkcorporation.lims.LimsApplication` |

The root package matches the Maven `groupId` (resolves the init spec's
§6.1 open question, 2026-05-04). This is the standard Maven convention
and avoids Spring component-scan collisions if another `org.sparkcorporation.*`
Spring Boot app ever shares the classpath.

## Module package layout

Each Spring Modulith module follows the same internal layout:

```
org.sparkcorporation.lims.<module>/
├── api/        public service interfaces + DTOs other modules consume; only thing exported
├── domain/     entities, value objects, domain services, events; package-private
├── infra/      JPA repositories, external clients, file adapters; package-private
└── web/        REST controllers, web DTOs, request validation; package-private
```

The `shared/` package holds the shared kernel: value objects (`Money`,
`LabCode`, `DailyId`, `ReferenceRange`, `PersonName`, `ContactInfo`), the
RFC 7807 problem-details handler, the audit skeleton, and the security
primitives. `shared/` is the one place outside a module that other modules
may depend on directly.

## Coding standards

### Records vs classes

- **Records** for immutable value objects (`Money`, `LabCode`, `DailyId`,
  `ReferenceRange`, `PersonName`, `ContactInfo`), all
  `*Command`/`*Request`/`*Response` DTOs, and all event payloads.
- **Plain classes** for JPA entities (records cannot be JPA entities
  cleanly). Entities use `@Getter @Setter @Builder` plus the
  package-private no-arg constructor JPA needs.
- **Sealed types** for closed sets of variants:
  - `ResultValue` permits
    `NumericResult | IntegerResult | TextResult | FormulaResult | FreeFormResult`
  - `Sample` permits `InLabSample | HomeSample | ExternalSample`
  - `NotificationRequest` permits
    `EmailRequest | WhatsAppRequest | SmsRequest`

  Pattern matching keeps handling exhaustive at compile time.

### Lombok scope

| Annotation | Allowed? | Where |
|---|---|---|
| `@Slf4j` | ✅ | Anywhere needing a logger |
| `@RequiredArgsConstructor` | ✅ | Service/component constructor injection |
| `@Getter` | ✅ | Entities, DTOs |
| `@Setter` | ✅ | Entities, DTOs (avoid on records — they're immutable) |
| `@Builder` | ✅ | Entities, DTOs |
| `@Data` | ❌ | Too broad — opt into specific annotations |
| `@AllArgsConstructor`, `@NoArgsConstructor` on records | ❌ | Not applicable |
| `@SneakyThrows` | ❌ | Handle or declare; never swallow |

### Null policy (strict)

- Repositories return `Optional<T>` for "may not exist" finders; throw
  `EntityNotFoundException` for `requireById`-style.
- Services never return `null`. Methods that can fail to find return
  `Optional<T>`; methods that must succeed throw.
- Collections never null — return `List.of()` / `Set.of()` for empty.
- Method parameters: annotate with `@NonNull` from **JSpecify**
  (`org.jspecify:jspecify`) where the contract demands it; otherwise
  validate at the controller boundary with Bean Validation. JSpecify is
  chosen over Lombok's, Jakarta's, Spring's, or the Checker Framework's
  variant because it is the modern, vendor-neutral standard targeted by
  IDE and tooling support around Java 25.

### Comment policy (strict)

- No narrative comments inside method bodies. Names + tests carry meaning.
  If a method needs explaining, rename or extract.
- *WHY*-comments only when the why is non-obvious: hidden constraint,
  subtle invariant, surprising behavior, workaround for a specific bug,
  regulatory rule. Cite the *why*, not the *what*.
- Javadoc allowed on public API contracts (the `api/` package of each
  module + REST controllers via OpenAPI annotations).
- No `// TODO` left in committed code; convert to a tracked issue or
  delete.
- No commented-out code; `git log` has the history.

### Other style rules

- One public top-level class per file.
- Method length: prefer < 30 lines; refactor when growing.
- Import order: java, jakarta, third-party, `org.sparkcorporation.lims`.
  No wildcard imports.
- `final` on classes that aren't designed for extension is encouraged; on
  locals is left to author judgment.

For the rationale behind each rule, see
`docs/superpowers/specs/2026-05-03-lims-init-design.md` §2.5.

## TDD discipline

Hard rules:

| Rule | Enforcement |
|---|---|
| No commit while any test is red or skipped | CI rejects; AI assistant refuses |
| No production code without a failing test first | AI assistant refuses; PR check verifies test+impl in same PR for the same module |
| Every PR must include test changes touching the same module(s) as production changes | PR diff check |
| No `@Disabled` / `@Ignore` in `main` | Spotless / CI rejection |

### Outside-in loop

```
acceptance test (red)         captures user-visible behavior, stays red until slice ships
   ↓
unit test (red)               drop into the domain layer for the missing collaborator
   ↓
implementation (green)        smallest code that satisfies the unit test, no speculation
   ↓
refactor                      clean naming/duplication; tests stay green
   ↓
climb back up                 when enough unit tests are green, the acceptance test goes green too
```

### Test pyramid (rough targets)

| Level | Tooling | Speed | Share |
|---|---|---|---|
| Unit (domain) | JUnit 5, AssertJ, Mockito | < 50 ms | ~70% |
| Slice (web/persistence) | `@WebMvcTest`, `@DataJpaTest`, MockMvc, Spring Security Test | < 500 ms | ~20% |
| Integration (module) | `@SpringBootTest` + Testcontainers (PostgreSQL, MinIO) | seconds | ~8% |
| Architecture | ArchUnit + `ApplicationModules.verify()` | seconds | ~1% |
| Acceptance / E2E | RestAssured against a running instance | seconds | ~1% |

### Test doubles

- **Mocks** (Mockito) only at layer seams (service mocks repository in a
  unit test).
- **In-memory fakes** preferred over mocks for repositories in domain
  tests.
- **Stubs** for outbound adapters (mail, WhatsApp, SMS).
- **Never mock** `Clock`, `UlidProvider`, `Counter` — inject deterministic
  test implementations.
- **Never mock** value objects or pure domain functions.

### Test data

- One `TestDataBuilder` per aggregate
  (`PatientBuilder.aPatient().withSex(F).build()`).
- Seed data via Liquibase changelogs scoped to context `dev,test` only.
- Datafaker for bulk realistic data in performance tests.
- A `LimsScenarios` helper composes builders into common situations.

Every PR must include test changes touching the same module(s) as
production changes. CI verifies this via diff inspection. Skipped tests
(`@Disabled`, `@Ignore`) are forbidden in `main`.

## Quality gates that block merge

A red gate fails the PR; the gate set evolves as the project matures
(Phase 0 wires the initial set; later phases add gates as new tooling
lands).

| Gate | Threshold |
|---|---|
| All tests green | 100% (no skipped) |
| JaCoCo line coverage | ≥ 80% overall, ≥ 95% domain services, 100% value objects |
| JaCoCo branch coverage | ≥ 75% overall, ≥ 90% domain services |
| Mutation score (PIT) | ≥ 75% on touched modules (PR build); all modules nightly |
| ArchUnit + Spring Modulith verify | green |
| Liquibase status | no edits to applied changesets |
| Liquibase smoke | `update → rollback → update` runs clean against an empty Postgres |
| OWASP Dependency-Check | no CVSS ≥ 7 |
| Trivy (image + filesystem) | no HIGH/CRITICAL |
| SonarQube quality gate | passes (becomes enforceable once Phase 0 wires SonarQube) |
| Spotless + Checkstyle | clean |
| OpenAPI snapshot | non-trivial API change must include an updated snapshot |

## Liquibase rules

- One file per logical change under
  `src/main/resources/db/changelog/changes/`.
- `id = <yyyyMMddHHmm>-<short-name>`, `author = samuel` (or contributor's
  git handle).
- **Every changeset declares a `rollback` block.** If a true rollback is
  impossible, declare `<empty/>` *and* add a comment explaining why.
- **Never edit a changeset that has been applied to a shared environment** —
  add a new one. Enforced by `mvn liquibase:status` in CI.
- Seed/test data lives in `src/main/resources/db/changelog/data/` under
  context `dev,test` only.
- `mvn liquibase:updateSQL` output is attached to every release artifact
  for DBA review before prod runs.

**Example changeset id:**
`202605031430-create-patients` (timestamp `yyyyMMddHHmm` + short
kebab-case description).

## Git workflow

### Branching

- Trunk-based on `main`. No long-lived develop/release/staging branches.
- Branch naming: `<initials>/<type>/<short-desc>` — e.g.,
  `sk/feat/patient-lab-code`, `sk/fix/catalog-null-ref-range`.
- Allowed types:
  `feat | fix | refactor | test | docs | chore | build | ci | perf`.
- Branches are short-lived. Target: open → merge in < 3 days.

### Commits

- Subject line: imperative, ≤ 72 chars, no trailing period, **no
  Conventional Commits prefix**.
  - Good: `Add lab-code generator to patient module`
  - Good: `Reject negative measured diameters in antibiogram interpreter`
  - Bad: `feat(patient): added lab code generator.`
  - Bad: `wip` / `fix stuff` / `update`
- Body (encouraged for non-trivial commits): wrap at 72 chars, explain
  *why*. Reference the test file that drove the change.
- Trailers: `Co-authored-by:` allowed; no `Generated-by:` / AI-attribution
  trailers.
- Signed commits required (CI rejects unsigned commits on `main`).

### PR lifecycle

1. Open PR — title = subject line of the top commit; description includes
   one-paragraph *why*, the first-failing test file, link to the design
   spec under `docs/superpowers/specs/` if applicable, and manual test
   steps for any UI/API surface change.
2. CI runs the full pipeline minus deploy stages.
3. Author self-reviews the diff before requesting review.
4. **Single approval** from any maintainer. **Exception:** changes
   touching `iam/`, `audit/`, `shared/security/`, any Liquibase changeset,
   or a dependency bump need a *second* approval.
5. **Rebase onto latest `main`** before merge; never merge `main` into the
   PR branch.
6. **Rebase and merge** (no squash, no merge commit). Linear history on
   `main`.
7. Delete the branch on merge.

## Releases (CalVer, Jenkins-driven)

- **CalVer scheme `YYYY.0M.<BUILD_NUMBER>`** — Jenkins computes the
  version at build time from the year, zero-padded month, and the
  monotonic Jenkins `BUILD_NUMBER`. Example: `2026.05.142`.
- Tag format: `v2026.05.142`, annotated and signed by Jenkins.
- Maven `revision`: parent POM declares
  `<revision>local-SNAPSHOT</revision>` for local/IDE dev. Jenkins
  overrides on every build via
  `-Drevision=${YEAR}.${MONTH}.${BUILD_NUMBER}` (release builds) or
  `-Drevision=${YEAR}.${MONTH}.${BUILD_NUMBER}-SNAPSHOT` (PR/dev builds).
  Devs never edit the POM for versioning.
- Hotfix policy: branch from a release tag, fix, push, Jenkins cuts a new
  monotonically-numbered tag.
- Every push to `main` triggers the staging deploy. Production deploys
  require the manual approval gate from
  `LIMS-Backend-Technical-Documentation.md` §17.4. **Jenkins always tags**;
  the manual gate authorizes the production *ship*, not the act of
  tagging.

The Jenkinsfile that implements this lives at the repo root once Phase 0
adds it.

## Pre-commit hooks

- Installed locally under `.githooks/`, checked into the repo.
- One-time setup: `git config core.hooksPath .githooks` (also documented
  in the local-environment section above).
- Runs: Spotless format check, Checkstyle, `mvn -B test -Punit`. Slow
  gates (integration, mutation, coverage) stay in CI.

The hooks themselves are checked in under `.githooks/` when Phase 0
scaffolds the build.

## Definition of Done

### Per-change DoD

- The failing test that motivated the change is green.
- All other tests are green.
- The slice has at least one happy-path acceptance test and one error-path
  test.
- Coverage thresholds met.
- Mutation score on touched files has not dropped.
- Liquibase rollback included for any new changeset.
- Commit body names the test file that *first failed* and now passes.

### Per-PR DoD

- [ ] Rebased onto latest `main`.
- [ ] All commits signed.
- [ ] No commits-while-red anywhere in the branch history.
- [ ] All CI green (tests, mutation, ArchUnit, Liquibase status + smoke,
      coverage gate, dependency-check, Trivy, SonarQube quality gate,
      Spotless, Checkstyle).
- [ ] If the PR changes the REST API surface: OpenAPI snapshot updated and
      reviewed.
- [ ] If the PR adds a Liquibase changeset: rollback block included and CI
      smoke passed.
- [ ] If the PR changes architecture, contracts, or stack versions:
      `LIMS-Backend-Technical-Documentation.md` is updated in the same
      PR.
- [ ] No PII added to log statements (review log diffs against §10/§16 of
      the LIMS doc).
- [ ] No secrets, `.env` files, or credentials committed.
- [ ] PR description includes the *first-failing test file* that the
      change makes pass.

## Logging hygiene

- JSON logs only.
- Mandatory MDC fields: `traceId`, `userId`, `labFileId` (when in scope).
- **Forbidden in log bodies:** patient names, ID document numbers, dates
  of birth, contact info, portal passwords, JWT tokens, raw
  request/response bodies for `/patients/*`, `/results/*`, `/lab-files/*`.
  Replace with the corresponding ULID at the controller layer before
  logging.

## Reporting security issues

- Do not file public issues for vulnerabilities.
- Email `klutse.samuel@gmail.com` (or the dedicated security alias once it
  exists) with a description and reproduction steps.
- Acknowledgment within 72 hours; coordinated disclosure timeline
  thereafter.

A formal `SECURITY.md` with public PGP key and disclosure policy will
land alongside Phase 1's `iam` module.

## Where to look next

- `LIMS-Backend-Technical-Documentation.md` — full technical spec
  (architecture, domain model, database, REST API, security, deployment).
- `CLAUDE.md` — distilled rules for AI assistants working in this repo.
- `docs/superpowers/specs/` — design specs for each module / feature, the
  source of all behavior changes.
- `docs/superpowers/plans/` — implementation plans derived from those
  specs.
- `~/.claude/projects/.../memory/` (developer-local) — persistent memory
  for the AI assistant; not version-controlled, but documents conventions
  decided per-project.
