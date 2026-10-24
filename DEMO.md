# Transact v2 Demo Walkthrough

A 3–5 minute portfolio demonstration of virtual funds only. Use fresh, clearly named demo accounts; the PostgreSQL database and integration-test rows persist between runs.

## 1. Launch and orient (30 seconds)

Start PostgreSQL and ensure the local `transact` database is configured. From the repository root, with the Git-ignored `application-local.properties` in place:

```powershell
.\mvnw.cmd clean package
java -jar target/transact-0.0.1-SNAPSHOT.jar --spring.config.additional-location=optional:file:./application-local.properties --server.port=18080
```

If Transact is already running from the JAR, stop that project process before rebuilding to avoid a Windows file lock. Open `http://localhost:18080/`. Explain that this is a virtual payment simulator, not a real payment service.

## 2. Overview (30–40 seconds)

Show the four cards: account count, completed transfers, total virtual amount transferred, and current account balances. Point to the seven-day UTC volume chart—including any zero-volume days—and the newest transactions. These values come from PostgreSQL-backed analytics APIs, not sample data embedded in the page.

## 3. Accounts and transfer (60–75 seconds)

In **Accounts**, create two fresh accounts with distinct names, such as `Demo Sender <date>` with `10.00` units and `Demo Receiver <date>` with `0.00`. Note their IDs and show that they appear immediately without reloading the browser.

In **Transfers**, select those accounts and send `1.25` virtual units. Show the success confirmation and refreshed balances: sender `8.75`, receiver `1.25`. Explain that validation rejects self-transfers, nonpositive amounts, and insufficient funds. Do not use existing user accounts for the demonstration.

## 4. History, details, and statements (60–75 seconds)

In **History**, search for the new transfer ID. Show the sender and receiver filters, date range, result count, and Previous/Next controls. Open **View details** to inspect the persisted amount, status, and timestamp; close the dialog with Escape.

Return to **Accounts** and open each new account’s statement. The sender entry is **DEBIT** and the receiver entry is **CREDIT**, with the opposite account as counterparty. Mention that longer statements use backend pagination.

## 5. Reliability and scope (45–60 seconds)

A transfer debits one account, credits the other, and inserts history in one database transaction. The service locks account rows in ascending ID order to prevent lost updates and reduce deadlock risk. PostgreSQL transaction-scoped advisory locks serialize requests sharing an idempotency key. If the browser cannot confirm a response, its retry action retains the original key and payload rather than risking a duplicate logical transfer.

Point to the PostgreSQL integration tests for concurrent transfers, idempotent replay, filtering, statements, and analytics. Close by stating the limits: no authentication, bank connection, real money, or production-readiness claim.
