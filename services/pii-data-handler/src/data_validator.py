from typing import Any


CUSTOMER_DATA_SCHEMA = {
    'required_fields': ['customer_id', 'first_name', 'last_name'],
    'optional_fields': [
        'email', 'email_address',
        'phone', 'phone_number', 'phoneNumber',
        'ssn', 'social_security', 'socialSecurity',
        'credit_card', 'creditCard', 'card_number',
        'address', 'date_of_birth',
    ],
    'field_types': {
        'customer_id': str,
        'first_name': str,
        'last_name': str,
        'email': str,
        'phone': str,
        'ssn': str,
        'credit_card': str,
    },
}


class DataValidator:
    """Validates incoming data against expected schema."""

    def validate(self, data: dict[str, Any]) -> tuple[bool, list[str]]:
        errors: list[str] = []

        if not isinstance(data, dict):
            return False, ['Input must be a JSON object']

        for field in CUSTOMER_DATA_SCHEMA['required_fields']:
            if field not in data:
                errors.append(f'Missing required field: {field}')
            elif not isinstance(data[field], str) or not data[field].strip():
                errors.append(f'Field {field} must be a non-empty string')

        all_known_fields = (
            CUSTOMER_DATA_SCHEMA['required_fields']
            + CUSTOMER_DATA_SCHEMA['optional_fields']
        )
        for field in data:
            if field not in all_known_fields:
                errors.append(f'Unknown field: {field}')

        is_valid = len(errors) == 0
        return is_valid, errors
