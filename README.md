# Transact

Transact is a small Spring Boot transaction system with PostgreSQL-backed demo accounts and atomic fund transfers. It is a learning project, not a payment gateway or banking integration.

## Stack

Java 21 target, Spring Boot 4.0.9, Maven, Spring Web MVC, Spring Data JPA, Bean Validation, PostgreSQL, and JUnit 5.

## Implemented

- Create a demo account with a nonnegative starting balance.
- List accounts and retrieve an account by ID.
- Validate inputs and return JSON errors for invalid requests and missing accounts.
- Transfer funds between accounts, with idempotent retries and transfer history.

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

Alternatively, put `spring.datasource.username` and `spring.datasource.password` in the Git-ignored `application-local.properties`, then run the packaged app with `--spring.config.additional-location=optional:file:./application-local.properties`. Add `--server.port=18080` if port 8080 is occupied.

## API

```http
POST /api/accounts
Content-Type: application/json

{"displayName":"Aviral","initialBalance":100.00}
```

Returns `201 Created`, a `Location` header, and an account with `id`, `displayName`, `balance`, and `createdAt`. Invalid input returns `400` with an `error` message.

`GET /api/accounts` returns an array of accounts. `GET /api/accounts/1` returns one account or `404` with an `error` message.

## Transfers

```http
POST /api/transfers
Idempotency-Key: order-123
Content-Type: application/json

{"fromAccountId":1,"toAccountId":2,"amount":25.00}
```

A new transfer returns `201 Created` and a `Location` header. Repeating the same key with the same accounts and equivalent amount returns the original transfer with `200 OK`; it does not move funds again. Reusing the key for a different transfer returns `409 Conflict`. Keys must have 1–128 ASCII letters, digits, dots, underscores, colons, or hyphens, beginning with a letter or digit. Whitespace around a key is trimmed. Amounts must be positive with at most two decimal places.

`GET /api/transfers` returns up to 100 completed transfers, newest first. `GET /api/transfers/{id}` returns one transfer or `404`.

Invalid input returns `400`; insufficient funds and conflicting keys return `409`; missing accounts or transfers return `404`. Error responses have an `error` field. Only completed transfers are stored.

Each transfer debits, credits, and saves its history record in one database transaction. PostgreSQL transaction-scoped advisory locks serialize requests with the same key before checking for an existing transfer; the database also has a unique key constraint. The two account rows are locked for writing in ascending ID order until commit, preventing lost updates and negative balances. Failed transactions roll back and do not consume their key.

To run the focused PostgreSQL concurrency tests locally with the ignored configuration file, use PowerShell:

```powershell
.\mvnw.cmd '-Dtransact.integration=true' '-Dspring.config.additional-location=optional:file:./application-local.properties' test
```
