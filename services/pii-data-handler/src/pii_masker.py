import re


class PIIMasker:
    """Masks PII fields in customer data."""

    def mask_credit_card(self, card_number: str) -> str:
        digits = re.sub(r'\D', '', card_number)
        if len(digits) < 4:
            return '****'
        masked = '*' * (len(digits) - 4) + digits[-4:]
        return masked

    def mask_ssn(self, ssn: str) -> str:
        digits = re.sub(r'\D', '', ssn)
        if len(digits) < 4:
            return '***-**-****'
        return '***-**-' + digits[-4:]

    def mask_email(self, email: str) -> str:
        if '@' not in email:
            return '****'
        local, domain = email.rsplit('@', 1)
        if len(local) <= 2:
            masked_local = '*' * len(local)
        else:
            masked_local = local[0] + '*' * (len(local) - 2) + local[-1]
        return f'{masked_local}@{domain}'

    def mask_phone(self, phone: str) -> str:
        digits = re.sub(r'\D', '', phone)
        if len(digits) < 4:
            return '****'
        return '*' * (len(digits) - 4) + digits[-4:]

    def mask_data(self, data: dict) -> dict:
        masked = data.copy()

        field_maskers = {
            'credit_card': self.mask_credit_card,
            'creditCard': self.mask_credit_card,
            'card_number': self.mask_credit_card,
            'ssn': self.mask_ssn,
            'social_security': self.mask_ssn,
            'socialSecurity': self.mask_ssn,
            'email': self.mask_email,
            'email_address': self.mask_email,
            'phone': self.mask_phone,
            'phone_number': self.mask_phone,
            'phoneNumber': self.mask_phone,
        }

        for field, masker in field_maskers.items():
            if field in masked and isinstance(masked[field], str):
                masked[field] = masker(masked[field])

        return masked
