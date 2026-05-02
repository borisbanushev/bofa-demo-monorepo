from flask import Flask, request, jsonify
from .pii_masker import PIIMasker
from .data_validator import DataValidator
from .audit_logger import AuditLogger

app = Flask(__name__)

masker = PIIMasker()
validator = DataValidator()
audit_logger = AuditLogger()


@app.route('/pii/process', methods=['POST'])
def process_pii():
    data = request.get_json()

    if not data:
        return jsonify({'error': 'Request body must be valid JSON'}), 400

    user_id = request.headers.get('X-User-ID', 'SYSTEM')

    is_valid, errors = validator.validate(data)
    if not is_valid:
        return jsonify({'error': 'Validation failed', 'details': errors}), 400

    masked_data = masker.mask_data(data)

    audit_entry = audit_logger.log_access(
        user_id=user_id,
        data_type='CUSTOMER_PII',
        masked_data=masked_data,
        action='PROCESS',
    )

    return jsonify({
        'masked_data': masked_data,
        'audit_id': audit_entry['audit_id'],
        'processed_at': audit_entry['timestamp'],
    })


if __name__ == '__main__':
    app.run(port=5001, debug=True)
