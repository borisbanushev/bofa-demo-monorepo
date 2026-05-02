import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from src.data_validator import DataValidator


class TestDataValidator:
    def setup_method(self):
        self.validator = DataValidator()

    def test_valid_data_passes_validation(self):
        data = {
            'customer_id': 'CUST-001',
            'first_name': 'John',
            'last_name': 'Doe',
            'email': 'john.doe@example.com',
            'ssn': '123-45-6789',
        }
        is_valid, errors = self.validator.validate(data)
        assert is_valid is True
        assert len(errors) == 0

    def test_missing_required_fields_fails_validation(self):
        data = {
            'first_name': 'John',
        }
        is_valid, errors = self.validator.validate(data)
        assert is_valid is False
        assert any('customer_id' in e for e in errors)
        assert any('last_name' in e for e in errors)
