import json
import logging
from datetime import datetime, timezone

logger = logging.getLogger('audit')
logger.setLevel(logging.INFO)

if not logger.handlers:
    handler = logging.StreamHandler()
    handler.setFormatter(logging.Formatter('%(message)s'))
    logger.addHandler(handler)


class AuditLogger:
    """Logs every PII access event to an audit trail."""

    def __init__(self):
        self.audit_trail: list[dict] = []

    def log_access(
        self,
        user_id: str,
        data_type: str,
        masked_data: dict,
        action: str = 'ACCESS',
    ) -> dict:
        entry = {
            'timestamp': datetime.now(timezone.utc).isoformat(),
            'user_id': user_id,
            'action': action,
            'data_type': data_type,
            'masked_representation': self._extract_masked_fields(masked_data),
            'audit_id': f'AUD-{len(self.audit_trail) + 1:06d}',
        }

        self.audit_trail.append(entry)
        logger.info(json.dumps(entry))

        return entry

    def get_audit_trail(self, user_id: str | None = None) -> list[dict]:
        if user_id:
            return [e for e in self.audit_trail if e['user_id'] == user_id]
        return list(self.audit_trail)

    def _extract_masked_fields(self, masked_data: dict) -> dict:
        pii_fields = [
            'credit_card', 'creditCard', 'card_number',
            'ssn', 'social_security', 'socialSecurity',
            'email', 'email_address',
            'phone', 'phone_number', 'phoneNumber',
        ]
        return {k: v for k, v in masked_data.items() if k in pii_fields}
