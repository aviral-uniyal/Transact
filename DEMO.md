# Transact Demo Walkthrough

This flow is designed for a 3-5 minute SDE internship demonstration. All balances are virtual.

## 1. Start the application

Ensure PostgreSQL is running and the local `transact` database is configured. With the Git-ignored `application-local.properties` file:

```powershell
.\mvnw.cmd clean package
java -jar target/transact-0.0.1-SNAPSHOT.jar --spring.config.additional-location=optional:file:./application-local.properties --server.port=18080
```

Open `http://localhost:18080/`.

## 2. Create two demo accounts

Use **Create account** to add:

- `Demo Sender` with `1000.00` virtual units
- `Demo Receiver` with `100.00` virtual units

Point out that balances are stored as decimal values in PostgreSQL, not calculated in the browser.

## 3. Transfer virtual funds

In **New transfer**:

1. Select `Demo Sender` as the sender.
2. Select `Demo Receiver` as the receiver.
3. Enter `250.00`.
4. Submit the transfer.

Expected balances:

- Demo Sender: `750.00`
- Demo Receiver: `350.00`

Show the completed entry in **Transfer history**. Explain that debit, credit, and history insertion commit as one database transaction.

## 4. Demonstrate rejection

Try transferring `1000.00` from Demo Sender to Demo Receiver. The API returns `409 Conflict` for insufficient funds, and both balances remain unchanged.

## 5. Explain idempotency

The dashboard creates an idempotency key for each logical transfer and preserves it if the result is uncertain. To demonstrate directly with the API, send the same request twice with the same key:

```http
POST /api/transfers
Content-Type: application/json
Idempotency-Key: interview-demo-001

{"fromAccountId":<sender-id>,"toAccountId":<receiver-id>,"amount":10.00}
```

The first request returns `201 Created`. The identical replay returns `200 OK` with the original transfer ID, and balances change only once. Reusing the key with a different amount returns `409 Conflict`.

## 6. Explain concurrency safety

The service locks both account rows in ascending account-ID order before checking or updating the sender balance. This prevents lost updates and reduces deadlock risk. Requests sharing an idempotency key are serialized with a PostgreSQL transaction-scoped advisory lock. Any exception rolls back both balance changes and the transfer record.

Finish by noting that the included PostgreSQL integration tests execute simultaneous shared-account transfers and simultaneous requests with the same idempotency key.
