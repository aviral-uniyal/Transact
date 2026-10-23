# Transact

Transact is a small Spring Boot payment system project. Milestone 1 provides PostgreSQL-backed demo accounts. Transfers and concurrent balance management are planned, not implemented.

## Stack

Java 21 target, Spring Boot 4.0.9, Maven, Spring Web MVC, Spring Data JPA, Bean Validation, PostgreSQL, and JUnit 5.

## Implemented

- Create a demo account with a nonnegative starting balance.
- List accounts and retrieve an account by ID.
- Validate inputs and return JSON errors for invalid requests and missing accounts.

Money uses `BigDecimal` and a PostgreSQL `numeric(19,2)` column. Hibernate creates or updates the development schema on startup (`ddl-auto=update`). This is intended for local development; use versioned migrations before production deployment.

## Run locally

Install a Java 21 JDK and PostgreSQL. Maven is supplied by the wrapper. Create a local database named `transact` using your PostgreSQL administration tool or `CREATE DATABASE transact;`. Set the following environment variables for the database user you configured:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/transact'
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = '<your local password>'
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

On macOS/Linux, use `./mvnw`. `DB_URL` and `DB_USERNAME` default to the local values shown; `DB_PASSWORD` has no default password. Keep credentials outside Git.

## API

```http
POST /api/accounts
Content-Type: application/json

{"displayName":"Aviral","initialBalance":100.00}
```

Returns `201 Created`, a `Location` header, and an account with `id`, `displayName`, `balance`, and `createdAt`. Invalid input returns `400` with an `error` message.

`GET /api/accounts` returns an array of accounts. `GET /api/accounts/1` returns one account or `404` with an `error` message.

## Planned for Milestone 2

Atomic transfers, idempotency, concurrency-safe balance changes, and transaction history.
