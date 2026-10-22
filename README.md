<a href="https://fireflysoftware.io/openplatform/">
<picture>
  <source media="(max-width: 640px) and (prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-dark.svg">
  <source media="(max-width: 640px)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-light.svg">
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-dark.svg">
  <img src="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-light.svg" alt="Firefly OpenPlatform - Domain Lending - Provisioning Risk" width="100%">
</picture>
</a>

# Firefly OpenPlatform - Domain Lending - Provisioning Risk

Developed by the Firefly Software Foundation. [dev@fireflysoftware.io](mailto:dev@fireflysoftware.io)

[Website](https://fireflysoftware.io/) · [OpenPlatform](https://fireflysoftware.io/openplatform/) · [Module catalogue](https://fireflysoftware.io/openplatform/modules/)

Domain layer microservice for provisioning cases within the Firefly platform. This service orchestrates create, update, status change, close, and query against `core-lending-provisioning-risk` with compensating saga steps.

Repository: [https://github.com/fireflysoftwaresolutions/domain-lending-provisioning-risk](https://github.com/fireflysoftwaresolutions/domain-lending-provisioning-risk)

---

## Overview

`domain-lending-provisioning-risk` sits between experience services and `core-lending-provisioning-risk`. Write operations run through the FireflyFramework saga engine. Queries use the CQRS query bus. The domain depends on `core-lending-provisioning-risk-sdk` only. It does not import another core's internal packages. Identifiers that point at parties, contracts, or products stay UUIDs owned by those cores.

It creates, updates, changes status, and closes provisioning cases through `core-lending-provisioning-risk`. Calculations, journals, risk assessments, and stage history stay on the core. This saga covers the provisioning case.

### Key features

- **Create** -- register a provisioning case through `core-lending-provisioning-risk`, with compensation that deletes the core write
- **Update** -- read the current provisioning case and write the new field values through `update` on `core-lending-provisioning-risk`. Status is left unchanged. Compensation writes the previous record back.
- **Status change** -- read the current provisioning case and update its status through `core-lending-provisioning-risk`. The provisioning core records stage history with a stage code and an ECL amount. That row is not a provisioning-status history, so the status saga does not write one.
- **Close** -- Close runs the status saga and writes RELEASED. The core has no cancel status. Remove deletes the case, which the core allows for every status. **Close** writes `RELEASED` when the core lifecycle allows that move.
- **Query** -- read one provisioning case by identifier
- **Compensation** -- a failed later step asks the core to undo the work already accepted
- **CQRS** -- commands and queries travel on separate buses
- **Reactive I/O** -- Spring WebFlux with virtual threads enabled

## Architecture

### Module structure

| Module | Purpose |
|--------|---------|
| `domain-lending-provisioning-risk-core` | Saga workflows, commands, handlers, and the application service |
| `domain-lending-provisioning-risk-interfaces` | Request contracts for a status change and a closing reason |
| `domain-lending-provisioning-risk-infra` | SDK client factory and downstream base-path configuration |
| `domain-lending-provisioning-risk-web` | Spring Boot application, REST API, actuator, and OpenAPI |
| `domain-lending-provisioning-risk-sdk` | Java client generated from the OpenAPI document |

### Dependency flow

```
web --> interfaces --> core --> infra --> core-lending-provisioning-risk-sdk
sdk (generated from openapi.yml)
```

### Saga map

| Saga | Core calls | Compensation |
|------|------------|--------------|
| `RegisterProvisioningCaseSaga` | `create` on `ProvisioningCaseApi` | Deletes the record |
| `UpdateProvisioningCaseSaga` | Reads the current record, then writes field changes through `update` on `ProvisioningCaseApi`. Status is left unchanged. | Writes the previous record back through `update` |
| `UpdateProvisioningCaseStatusSaga` | Reads the current record, updates its status through `update` | Writes the previous status back through the core |
| Query | `getById` on `ProvisioningCaseApi` | None. A read has nothing to undo. |

Status values accepted by the core: `ACTIVE`, `RELEASED`, `WRITTEN_OFF`, `RECOVERED`.

## Setup

### Prerequisites

- Java 25
- Apache Maven 3.9+
- `core-lending-provisioning-risk-sdk` 26.10.6
- Apache Kafka on `localhost:9092` when event publishing is left at the default

### Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_ADDRESS` | `localhost` | Bind address |
| `SERVER_PORT` | `8049` | HTTP port |
| `ENDPOINT_CORE_LENDING_PROVISIONING` | `http://localhost:8080` | Base URL of `core-lending-provisioning-risk` |

### Build and test

```bash
mvn clean verify
```

### Run

```bash
ENDPOINT_CORE_LENDING_PROVISIONING=http://localhost:8080 mvn spring-boot:run -pl domain-lending-provisioning-risk-web
```

Swagger UI (non-production): [http://localhost:8049/swagger-ui.html](http://localhost:8049/swagger-ui.html)

## API

| Method | Path | Behavior |
|--------|------|----------|
| `POST` | `/api/v1/provisioning-cases` | Runs `RegisterProvisioningCaseSaga` |
| `GET` | `/api/v1/provisioning-cases/{provisioningCaseId}` | Queries the core |
| `PUT` | `/api/v1/provisioning-cases/{provisioningCaseId}/status` | Runs `UpdateProvisioningCaseStatusSaga` |
| `PUT` | `/api/v1/provisioning-cases/{provisioningCaseId}` | Runs `UpdateProvisioningCaseSaga` |
| `POST` | `/api/v1/provisioning-cases/{provisioningCaseId}/close` | Close provisioning case. Runs the status saga when `core-lending-provisioning-risk` allows `RELEASED`. |
| `DELETE` | `/api/v1/provisioning-cases/{provisioningCaseId}` | Deletes the record through `core-lending-provisioning-risk` when that core allows removal |

The status request body is `status` plus an optional `statusReason`.

Close writes RELEASED. The provisioning core does not restrict which status can follow another and has no cancel status. Remove deletes any case because the core delete has no status guard.
A closing command body is an optional `statusReason`.
