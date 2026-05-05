# LIMS — Laboratory Information Management System

Backend for a Laboratory Information Management System for medical
laboratories operating in West Africa (Togo, Benin). Spring Boot 4.0.6
modular monolith on Java 25, Spring Modulith, PostgreSQL 17.

This is the Phase 0 walking skeleton — the build, test, CI, and module
boundaries are wired; no business behavior is implemented yet.

## Quick start

```bash
git clone <this repo>
cd laboratory-information-management-system
git config core.hooksPath .githooks    # one-time: enable pre-commit hook

# Local dev stack (Postgres + MinIO)
docker compose up -d

# Build, lint, run all tests (unit + integration)
./mvnw clean verify

# Run the app
./mvnw spring-boot:run
# -> http://localhost:8080/actuator/health  =>  {"status":"UP"}
```

## Where to look next

| What | Where |
|---|---|
| What the system *is* (architecture, domain, DB, REST) | `LIMS-Backend-Technical-Documentation.md` |
| How we work (commits, reviews, releases, DoD) | `CONTRIBUTING.md` |
| AI-assistant directive (distilled rules) | `CLAUDE.md` |
| Active design specs and plans | `docs/superpowers/specs/`, `docs/superpowers/plans/` |

## License

Proprietary. No `LICENSE` file is committed; this repo is closed-source
until a deliberate licensing decision is made.
