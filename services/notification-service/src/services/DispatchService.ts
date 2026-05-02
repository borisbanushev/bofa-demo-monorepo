import { NotificationPayload } from '../models/notification';

export class DispatchService {
  dispatch(notification: NotificationPayload): { success: boolean; dispatchId: string } {
    console.log(`[DISPATCH] Sending ${notification.type} notification to customer ${notification.customerId}`);
    console.log(`[DISPATCH] Subject: ${notification.subject}`);
    console.log(`[DISPATCH] Priority: ${notification.priority}`);
    console.log(`[DISPATCH] Notification ID: ${notification.notificationId}`);
    console.log(`[DISPATCH] Timestamp: ${notification.timestamp}`);

    return {
      success: true,
      dispatchId: `DISP-${Date.now()}`,
    };
  }
}
