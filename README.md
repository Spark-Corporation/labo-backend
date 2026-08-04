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
```

### Run the whole stack in containers

`docker compose up` runs the full stack: the backend, Postgres, and MinIO.
The image is built from a pre-built jar, so package it first:

```bash
./mvnw clean package          # produces target/lims-backend-*.jar
docker compose up --build -d  # app + postgres + minio
# -> http://localhost:8080/actuator/health  =>  {"status":"UP"}

docker compose down           # stop the stack (add -v to drop the DB/MinIO data)
```

The app runs the `dev` profile (bootstrap admin `admin@local.dev`). Postgres
pre-creates the `lims_liquibase` metadata schema on first start via
`docker/postgres-init/init-schemas.sql`; Liquibase creates the rest.

### Dev loop (run the app from source)

Start only the dependencies and run the backend from your IDE or Maven:

```bash
docker compose up -d postgres minio    # dependencies only

# Build, lint, run all tests (unit + integration)
./mvnw clean verify

# Run the app
./mvnw spring-boot:run
# -> http://localhost:8080/actuator/health  =>  {"status":"UP"}
```

## API documentation

With the app running under the `dev` profile:

| What                       | Where                                         |
| -------------------------- | --------------------------------------------- |
| Swagger UI (try-it console) | <http://localhost:8080/swagger-ui.html>       |
| OpenAPI 3.1 schema          | <http://localhost:8080/v3/api-docs>           |
| Schema as a file            | `target/openapi.json`, written by `./mvnw verify` |

Operations are grouped by module and each one carries an `x-lims-permissions`
extension naming the permission its caller must hold — drive frontend gating off
those rather than off role names.

**Both endpoints are disabled unless the profile enables them** (`dev` turns on
the UI and the schema, `test` the schema only). A production boot exposes
neither. See §7.5 of the technical documentation.

`docs/API.md` is a hand-written snapshot of the same surface, kept for now as a
reference for readers without a running backend. The generated schema is
authoritative where the two disagree.

## Where to look next

| What                                                  | Where                                     |
| ----------------------------------------------------- | ----------------------------------------- |
| What the system _is_ (architecture, domain, DB, REST) | `LIMS-Backend-Technical-Documentation.md` |
| How we work (commits, reviews, releases, DoD)         | `CONTRIBUTING.md`                         |
| Every endpoint, hand-written                          | `docs/API.md`                             |

## License

Proprietary. No `LICENSE` file is committed; this repo is closed-source
until a deliberate licensing decision is made.
