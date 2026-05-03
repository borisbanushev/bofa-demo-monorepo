import request from 'supertest';
import { app } from '../src/index';

/**
 * Compliance-critical integration tests for NotificationService endpoints:
 * fraud alert dispatch and regulatory disclosure validation.
 * OCC regulatory examination artifact.
 */
describe('NotificationService Endpoints — Compliance-Critical Paths', () => {
  describe('POST /notify/fraud-alert — Fraud Alert Dispatch', () => {
    const validFraudPayload = {
      customerId: 'CUST-001',
      transactionId: 'TXN-FRAUD-001',
      transactionAmount: 4999.99,
      merchantName: 'Suspicious Merchant LLC',
      transactionDate: '2024-03-15',
      cardLastFour: '9876',
    };

    test('returns 200 with success for valid fraud alert', async () => {
      const response = await request(app)
        .post('/notify/fraud-alert')
        .send(validFraudPayload)
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.dispatchId).toBeDefined();
      expect(response.body.notificationId).toBeDefined();
      expect(response.body.type).toBe('FRAUD_ALERT');
      expect(response.body.priority).toBe('HIGH');
    });

    test('returns 400 when customerId is missing', async () => {
      const { customerId, ...missingCustomer } = validFraudPayload;
      const response = await request(app)
        .post('/notify/fraud-alert')
        .send(missingCustomer)
        .expect(400);

      expect(response.body.error).toContain('Missing required fields');
    });

    test('returns 400 when transactionId is missing', async () => {
      const { transactionId, ...missingTxn } = validFraudPayload;
      const response = await request(app)
        .post('/notify/fraud-alert')
        .send(missingTxn)
        .expect(400);

      expect(response.body.error).toContain('Missing required fields');
    });

    test('returns 400 when both customerId and transactionId are missing', async () => {
      const response = await request(app)
        .post('/notify/fraud-alert')
        .send({
          transactionAmount: 100,
          merchantName: 'Test',
          transactionDate: '2024-01-01',
          cardLastFour: '1234',
        })
        .expect(400);

      expect(response.body.error).toContain('Missing required fields');
    });

    test('dispatches with correct HIGH priority for fraud alerts', async () => {
      const response = await request(app)
        .post('/notify/fraud-alert')
        .send(validFraudPayload)
        .expect(200);

      expect(response.body.priority).toBe('HIGH');
    });

    test('handles large transaction amounts', async () => {
      const largeAmount = { ...validFraudPayload, transactionAmount: 999999.99 };
      const response = await request(app)
        .post('/notify/fraud-alert')
        .send(largeAmount)
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.type).toBe('FRAUD_ALERT');
    });
  });

  describe('POST /notify/regulatory-disclosure — Regulatory Disclosure Validation', () => {
    const validDisclosurePayload = {
      customerId: 'CUST-001',
      disclosureType: 'PRIVACY_POLICY_UPDATE',
      effectiveDate: '2024-04-01',
      regulatoryBody: 'OCC',
      disclosureText: 'Your privacy policy has been updated per new federal regulations.',
      acknowledgmentRequired: true,
      referenceNumber: 'REG-2024-001',
    };

    test('returns 200 with success for valid disclosure', async () => {
      const response = await request(app)
        .post('/notify/regulatory-disclosure')
        .send(validDisclosurePayload)
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.dispatchId).toBeDefined();
      expect(response.body.notificationId).toBeDefined();
      expect(response.body.type).toBe('REGULATORY_DISCLOSURE');
      expect(response.body.priority).toBe('MEDIUM');
    });

    test('returns 400 when customerId is missing', async () => {
      const { customerId, ...missingCustomer } = validDisclosurePayload;
      const response = await request(app)
        .post('/notify/regulatory-disclosure')
        .send(missingCustomer)
        .expect(400);

      expect(response.body.error).toContain('Missing required field: customerId');
    });

    test('returns 400 when disclosureType is missing', async () => {
      const invalid = { ...validDisclosurePayload, disclosureType: '' };
      const response = await request(app)
        .post('/notify/regulatory-disclosure')
        .send(invalid)
        .expect(400);

      expect(response.body.error).toContain('Missing required regulatory field: disclosureType');
    });

    test('returns 400 when effectiveDate is missing', async () => {
      const invalid = { ...validDisclosurePayload, effectiveDate: '' };
      const response = await request(app)
        .post('/notify/regulatory-disclosure')
        .send(invalid)
        .expect(400);

      expect(response.body.error).toContain('Missing required regulatory field: effectiveDate');
    });

    test('returns 400 when regulatoryBody is missing', async () => {
      const invalid = { ...validDisclosurePayload, regulatoryBody: '' };
      const response = await request(app)
        .post('/notify/regulatory-disclosure')
        .send(invalid)
        .expect(400);

      expect(response.body.error).toContain('Missing required regulatory field: regulatoryBody');
    });

    test('returns 400 when disclosureText is missing', async () => {
      const invalid = { ...validDisclosurePayload, disclosureText: '' };
      const response = await request(app)
        .post('/notify/regulatory-disclosure')
        .send(invalid)
        .expect(400);

      expect(response.body.error).toContain('Missing required regulatory field: disclosureText');
    });

    test('returns 400 when referenceNumber is missing', async () => {
      const invalid = { ...validDisclosurePayload, referenceNumber: '' };
      const response = await request(app)
        .post('/notify/regulatory-disclosure')
        .send(invalid)
        .expect(400);

      expect(response.body.error).toContain('Missing required regulatory field: referenceNumber');
    });

    test('accepts disclosure where acknowledgment is not required', async () => {
      const noAck = { ...validDisclosurePayload, acknowledgmentRequired: false };
      const response = await request(app)
        .post('/notify/regulatory-disclosure')
        .send(noAck)
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.type).toBe('REGULATORY_DISCLOSURE');
    });
  });

  describe('DispatchService — Fraud Alert Dispatch', () => {
    test('dispatch returns success and dispatchId for fraud alert payload', async () => {
      const response = await request(app)
        .post('/notify/fraud-alert')
        .send({
          customerId: 'CUST-002',
          transactionId: 'TXN-FRAUD-002',
          transactionAmount: 250.00,
          merchantName: 'Unknown Vendor',
          transactionDate: '2024-06-01',
          cardLastFour: '1111',
        })
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.dispatchId).toMatch(/^DISP-/);
    });

    test('dispatch returns success and dispatchId for regulatory disclosure payload', async () => {
      const response = await request(app)
        .post('/notify/regulatory-disclosure')
        .send({
          customerId: 'CUST-002',
          disclosureType: 'TERMS_OF_SERVICE',
          effectiveDate: '2024-07-01',
          regulatoryBody: 'CFPB',
          disclosureText: 'Terms of service updated.',
          acknowledgmentRequired: false,
          referenceNumber: 'REG-2024-002',
        })
        .expect(200);

      expect(response.body.success).toBe(true);
      expect(response.body.dispatchId).toMatch(/^DISP-/);
    });
  });
});
