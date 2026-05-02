# Notification Service

This service handles real-time customer notifications including fraud alerts, transaction confirmations, and regulatory disclosures for the digital banking platform.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/notify/fraud-alert` | Dispatch a real-time fraud alert notification |
| POST | `/notify/transaction-confirm` | Send a transaction confirmation notification |
| POST | `/notify/regulatory-disclosure` | Format and send a regulatory disclosure notification |

## Architecture

- **NotificationFormatter** — Handles message formatting for each notification type (fraud alert, transaction confirmation, regulatory disclosure)
- **DispatchService** — Handles the actual dispatch of notifications (mock implementation — logs the notification)

## Compliance-Critical Paths

The following paths are compliance-critical and require thorough testing and monitoring:

- **Fraud alert dispatch** — Real-time fraud alert notifications must be dispatched immediately and contain all required transaction details for customer verification
- **Regulatory disclosure formatting** — Regulatory disclosure notifications must include specific required fields per compliance standards (disclosure type, effective date, regulatory body, disclosure text, reference number)

## Running

```bash
npm install
npm run dev
```

## Testing

```bash
npm test
```

## Tech Stack

- Node.js with TypeScript
- Express.js
- Jest (test framework)
- Supertest (HTTP assertion library)
