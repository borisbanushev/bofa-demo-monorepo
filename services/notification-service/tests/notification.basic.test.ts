import request from 'supertest';
import { app } from '../src/index';
import { DispatchService } from '../src/services/DispatchService';

describe('NotificationService Basic Tests', () => {
  test('POST /notify/transaction-confirm responds with 200', async () => {
    const response = await request(app)
      .post('/notify/transaction-confirm')
      .send({
        customerId: 'CUST-001',
        transactionId: 'TXN-12345',
        amount: 150.00,
        merchantName: 'Amazon',
        transactionDate: '2024-03-15',
        accountLastFour: '4567',
      })
      .expect(200);

    expect(response.body.success).toBe(true);
    expect(response.body.notificationId).toBeDefined();
    expect(response.body.type).toBe('TRANSACTION_CONFIRMATION');
  });

  test('DispatchService logs a message', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    const dispatcher = new DispatchService();

    const result = dispatcher.dispatch({
      notificationId: 'TEST-001',
      type: 'TRANSACTION_CONFIRMATION',
      customerId: 'CUST-001',
      subject: 'Test Subject',
      body: 'Test Body',
      priority: 'LOW',
      timestamp: new Date().toISOString(),
      metadata: {},
    });

    expect(result.success).toBe(true);
    expect(result.dispatchId).toBeDefined();
    expect(consoleSpy).toHaveBeenCalled();

    consoleSpy.mockRestore();
  });
});
