import express from 'express';
import { NotificationFormatter } from './services/NotificationFormatter';
import { DispatchService } from './services/DispatchService';
import {
  FraudAlertRequest,
  TransactionConfirmRequest,
  RegulatoryDisclosureRequest,
} from './models/notification';

const app = express();
app.use(express.json());

const formatter = new NotificationFormatter();
const dispatcher = new DispatchService();

app.post('/notify/fraud-alert', (req, res) => {
  try {
    const request: FraudAlertRequest = req.body;

    if (!request.customerId || !request.transactionId) {
      res.status(400).json({ error: 'Missing required fields: customerId, transactionId' });
      return;
    }

    const notification = formatter.formatFraudAlert(request);
    const result = dispatcher.dispatch(notification);

    res.json({
      ...result,
      notificationId: notification.notificationId,
      type: notification.type,
      priority: notification.priority,
    });
  } catch (error) {
    res.status(500).json({ error: 'Failed to process fraud alert notification' });
  }
});

app.post('/notify/transaction-confirm', (req, res) => {
  try {
    const request: TransactionConfirmRequest = req.body;

    if (!request.customerId || !request.transactionId) {
      res.status(400).json({ error: 'Missing required fields: customerId, transactionId' });
      return;
    }

    const notification = formatter.formatTransactionConfirm(request);
    const result = dispatcher.dispatch(notification);

    res.json({
      ...result,
      notificationId: notification.notificationId,
      type: notification.type,
    });
  } catch (error) {
    res.status(500).json({ error: 'Failed to process transaction confirmation notification' });
  }
});

app.post('/notify/regulatory-disclosure', (req, res) => {
  try {
    const request: RegulatoryDisclosureRequest = req.body;

    if (!request.customerId) {
      res.status(400).json({ error: 'Missing required field: customerId' });
      return;
    }

    const notification = formatter.formatRegulatoryDisclosure(request);
    const result = dispatcher.dispatch(notification);

    res.json({
      ...result,
      notificationId: notification.notificationId,
      type: notification.type,
      priority: notification.priority,
    });
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    res.status(400).json({ error: errorMessage });
  }
});

const PORT = process.env.PORT || 3001;

if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`Notification Service running on port ${PORT}`);
  });
}

export { app };
