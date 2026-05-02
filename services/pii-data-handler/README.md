# PII Data Handler

This service handles PII (Personally Identifiable Information) data processing for the digital banking platform. It provides data masking, validation, and audit logging capabilities.

## Endpoint

| Method | Path | Description |
|--------|------|-------------|
| POST | `/pii/process` | Accept raw data, validate, mask PII, log access, return masked data |

## Architecture

- **PIIMasker** (`pii_masker.py`) — Masks PII fields including credit card numbers, SSNs, email addresses, and phone numbers
- **DataValidator** (`data_validator.py`) — Validates incoming data against expected schema
- **AuditLogger** (`audit_logger.py`) — Logs every PII access event to an audit trail with timestamp, user ID, data type, and masked representation
- **API** (`api.py`) — Flask endpoint that orchestrates validation, masking, and audit logging

## PII Masking Rules

| Field | Masking Rule |
|-------|-------------|
| Credit Card | Mask all but last 4 digits |
| SSN | Mask all but last 4 digits (format: `***-**-XXXX`) |
| Email | Mask the local part (e.g., `j***e@example.com`) |
| Phone | Mask all but last 4 digits |

## Compliance-Critical Paths

The following paths are compliance-critical and require thorough testing and monitoring:

- **PII masking logic** — Ensures all PII fields are properly masked before any data is returned or stored
- **Audit logging** — Every PII data access event must be logged with timestamp, user ID, data type accessed, and masked representation

## Running

```bash
pip install -r requirements.txt
python -m src.api
```

## Testing

```bash
pytest tests/
```

## Tech Stack

- Python 3.12
- Flask 3.0.2
- pytest 8.0.2
