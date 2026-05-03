import { NotificationFormatter } from '../src/services/NotificationFormatter';
import {
  FraudAlertRequest,
  RegulatoryDisclosureRequest,
} from '../src/models/notification';

/**
 * Compliance-critical unit tests for NotificationFormatter:
 * fraud alert dispatch formatting and regulatory disclosure field validation.
 * OCC regulatory examination artifact.
 */
describe('NotificationFormatter — Compliance-Critical Paths', () => {
  let formatter: NotificationFormatter;

  beforeEach(() => {
    formatter = new NotificationFormatter();
  });

  describe('Fraud Alert Formatting', () => {
    const validFraudRequest: FraudAlertRequest = {
      customerId: 'CUST-001',
      transactionId: 'TXN-FRAUD-001',
      transactionAmount: 4999.99,
      merchantName: 'Suspicious Merchant LLC',
      transactionDate: '2024-03-15',
      cardLastFour: '9876',
    };

    test('sets notification type to FRAUD_ALERT', () => {
      const payload = formatter.formatFraudAlert(validFraudRequest);
      expect(payload.type).toBe('FRAUD_ALERT');
    });

    test('sets priority to HIGH', () => {
      const payload = formatter.formatFraudAlert(validFraudRequest);
      expect(payload.priority).toBe('HIGH');
    });

    test('generates a unique notificationId', () => {
      const payload1 = formatter.formatFraudAlert(validFraudRequest);
      const payload2 = formatter.formatFraudAlert(validFraudRequest);
      expect(payload1.notificationId).toBeDefined();
      expect(payload2.notificationId).toBeDefined();
      expect(payload1.notificationId).not.toBe(payload2.notificationId);
    });

    test('includes card last four in subject', () => {
      const payload = formatter.formatFraudAlert(validFraudRequest);
      expect(payload.subject).toContain('9876');
    });

    test('includes transaction amount in body formatted to 2 decimals', () => {
      const payload = formatter.formatFraudAlert(validFraudRequest);
      expect(payload.body).toContain('$4999.99');
    });

    test('includes merchant name in body', () => {
      const payload = formatter.formatFraudAlert(validFraudRequest);
      expect(payload.body).toContain('Suspicious Merchant LLC');
    });

    test('includes transaction date in body', () => {
      const payload = formatter.formatFraudAlert(validFraudRequest);
      expect(payload.body).toContain('2024-03-15');
    });

    test('includes transaction ID in body', () => {
      const payload = formatter.formatFraudAlert(validFraudRequest);
      expect(payload.body).toContain('TXN-FRAUD-001');
    });

    test('includes emergency contact number in body', () => {
      const payload = formatter.formatFraudAlert(validFraudRequest);
      expect(payload.body).toContain('1-800-BANK-SEC');
    });

    test('preserves customerId', () => {
      const payload = formatter.formatFraudAlert(validFraudRequest);
      expect(payload.customerId).toBe('CUST-001');
    });

    test('sets timestamp as ISO string', () => {
      const payload = formatter.formatFraudAlert(validFraudRequest);
      expect(() => new Date(payload.timestamp)).not.toThrow();
      expect(new Date(payload.timestamp).toISOString()).toBe(payload.timestamp);
    });

    test('populates metadata with transactionId, merchantName, amount, cardLastFour', () => {
      const payload = formatter.formatFraudAlert(validFraudRequest);
      expect(payload.metadata.transactionId).toBe('TXN-FRAUD-001');
      expect(payload.metadata.merchantName).toBe('Suspicious Merchant LLC');
      expect(payload.metadata.amount).toBe('4999.99');
      expect(payload.metadata.cardLastFour).toBe('9876');
    });
  });

  describe('Regulatory Disclosure Validation', () => {
    const validDisclosure: RegulatoryDisclosureRequest = {
      customerId: 'CUST-001',
      disclosureType: 'PRIVACY_POLICY_UPDATE',
      effectiveDate: '2024-04-01',
      regulatoryBody: 'OCC',
      disclosureText: 'Your privacy policy has been updated per new federal regulations.',
      acknowledgmentRequired: true,
      referenceNumber: 'REG-2024-001',
    };

    test('sets notification type to REGULATORY_DISCLOSURE', () => {
      const payload = formatter.formatRegulatoryDisclosure(validDisclosure);
      expect(payload.type).toBe('REGULATORY_DISCLOSURE');
    });

    test('sets priority to MEDIUM', () => {
      const payload = formatter.formatRegulatoryDisclosure(validDisclosure);
      expect(payload.priority).toBe('MEDIUM');
    });

    test('includes disclosure type in subject', () => {
      const payload = formatter.formatRegulatoryDisclosure(validDisclosure);
      expect(payload.subject).toContain('PRIVACY_POLICY_UPDATE');
    });

    test('includes disclosure text in body', () => {
      const payload = formatter.formatRegulatoryDisclosure(validDisclosure);
      expect(payload.body).toContain('Your privacy policy has been updated per new federal regulations.');
    });

    test('includes effective date in body', () => {
      const payload = formatter.formatRegulatoryDisclosure(validDisclosure);
      expect(payload.body).toContain('2024-04-01');
    });

    test('includes regulatory body in body', () => {
      const payload = formatter.formatRegulatoryDisclosure(validDisclosure);
      expect(payload.body).toContain('OCC');
    });

    test('includes reference number in body', () => {
      const payload = formatter.formatRegulatoryDisclosure(validDisclosure);
      expect(payload.body).toContain('REG-2024-001');
    });

    test('includes acknowledgment-required message when true', () => {
      const payload = formatter.formatRegulatoryDisclosure(validDisclosure);
      expect(payload.body).toContain('Your acknowledgment is required');
    });

    test('includes no-action-required message when acknowledgment is false', () => {
      const noAckRequest = { ...validDisclosure, acknowledgmentRequired: false };
      const payload = formatter.formatRegulatoryDisclosure(noAckRequest);
      expect(payload.body).toContain('No action required');
    });

    test('populates metadata with disclosureType, regulatoryBody, referenceNumber, acknowledgmentRequired', () => {
      const payload = formatter.formatRegulatoryDisclosure(validDisclosure);
      expect(payload.metadata.disclosureType).toBe('PRIVACY_POLICY_UPDATE');
      expect(payload.metadata.regulatoryBody).toBe('OCC');
      expect(payload.metadata.referenceNumber).toBe('REG-2024-001');
      expect(payload.metadata.acknowledgmentRequired).toBe('true');
    });

    test('throws error when disclosureType is missing', () => {
      const invalid = { ...validDisclosure, disclosureType: '' };
      expect(() => formatter.formatRegulatoryDisclosure(invalid)).toThrow('Missing required regulatory field: disclosureType');
    });

    test('throws error when effectiveDate is missing', () => {
      const invalid = { ...validDisclosure, effectiveDate: '' };
      expect(() => formatter.formatRegulatoryDisclosure(invalid)).toThrow('Missing required regulatory field: effectiveDate');
    });

    test('throws error when regulatoryBody is missing', () => {
      const invalid = { ...validDisclosure, regulatoryBody: '' };
      expect(() => formatter.formatRegulatoryDisclosure(invalid)).toThrow('Missing required regulatory field: regulatoryBody');
    });

    test('throws error when disclosureText is missing', () => {
      const invalid = { ...validDisclosure, disclosureText: '' };
      expect(() => formatter.formatRegulatoryDisclosure(invalid)).toThrow('Missing required regulatory field: disclosureText');
    });

    test('throws error when referenceNumber is missing', () => {
      const invalid = { ...validDisclosure, referenceNumber: '' };
      expect(() => formatter.formatRegulatoryDisclosure(invalid)).toThrow('Missing required regulatory field: referenceNumber');
    });
  });
});
