# Repository Guidelines

## Project Structure & Module Organization

SmartOmni is a Java 17/Spring Boot 3 Maven monorepo with shared libraries, seven Java domain services, and a Java Quartz worker. `smartomni-common` holds shared tenant, JWT, entity, exception, and response utilities. `nginx/` is the API gateway; `smartomni-jobs/` schedules background work; `smartomni-service-{auth,tenant,catalog,inventory,order,integration,ai}` implement domain services. `smartomni-gateway/` is retired legacy code outside the active Maven build. The Python FastAPI AI core lives outside this repository.

Java sources reside in each module's `src/main/java/com/smartomni/<domain>/`, organized into `controller`, `service`, `repository`, `entity`, `dto`, and `config` packages. Configuration lives in `src/main/resources/application.yml`. Place tests in `src/test/java` and test fixtures in `src/test/resources`. Architecture and schema references are in `docs/architecture.md` and `docs/erd.dbml`; deployment uses root `docker-compose.yml` and module Dockerfiles.

## Build, Test, and Development Commands

Use JDK 17+, Maven 3.9+, and Docker Compose. Run from the repository root unless noted:

- `mvn clean install` / `make build`: build, test, and install all modules.
- `mvn test` / `make test`: run unit tests across modules.
- `mvn -B clean verify`: reproduce the CI build.
- `make infra-up`: start PostgreSQL, Redis, and RabbitMQ.
- `mvn spring-boot:run` inside a service directory: run that service after installing shared dependencies.
- `make up`: build images and start the complete stack; `make logs` follows logs and `make down` stops it.

## Coding Style & Naming Conventions

Follow existing four-space Java indentation. Use PascalCase classes, camelCase methods/fields, and lowercase packages. Prefer descriptive suffixes such as `Controller`, `Service`, `Repository`, and `Request`. Keep DTOs separate from JPA entities; use `ApiResponse<T>` and business exceptions derived from `BusinessException`. No formatter or lint plugin is configured.

## Testing Guidelines

The parent POM supplies Spring Boot Test, including JUnit Jupiter and Mockito. `smartomni-migration-tests` verifies migrations and RLS with PostgreSQL 16; `mvn verify` also starts packaged services. No coverage threshold is enforced. Add `*Test.java` classes for new logic, covering validation, failures, and tenant isolation. Run `mvn test`; CI uploads Surefire reports.

## Commit & Pull Request Guidelines

History currently contains only an initial commit. Follow `CONTRIBUTING.md`: Conventional Commits such as `feat(order): add webhook idempotency`. Use `feature/<service>-<description>` or `fix/<service>-<description>` branches. Describe changes and validation in PRs, update affected API/architecture documentation, and obtain at least one teammate approval before merging into `develop`. Keep `main` deployable.

## Security & Tenant Isolation

Tenant-owned business entities must extend `BaseTenantEntity`. Use `TenantContext` and scope queries by tenant; never hardcode tenant IDs. Never commit `.env`, credentials, or real JWT secrets. Schema changes belong in `smartomni-db-migrations/src/main/resources/db/migration/`; never edit applied versions. Keep Hibernate `validate`. Follow `docs/database-migrations.md` for credentials and legacy upgrades.
