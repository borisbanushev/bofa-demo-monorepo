import { v4 as uuidv4 } from 'uuid';
import {
  FraudAlertRequest,
  TransactionConfirmRequest,
  RegulatoryDisclosureRequest,
  NotificationPayload,
} from '../models/notification';

export class NotificationFormatter {
  formatFraudAlert(request: FraudAlertRequest): NotificationPayload {
    return {
      notificationId: uuidv4(),
      type: 'FRAUD_ALERT',
      customerId: request.customerId,
      subject: `Suspicious Transaction Detected — Card ending ${request.cardLastFour}`,
      body: `A suspicious transaction of $${request.transactionAmount.toFixed(2)} at ${request.merchantName} on ${request.transactionDate} has been flagged. Transaction ID: ${request.transactionId}. If you did not authorize this transaction, please contact us immediately at 1-800-BANK-SEC.`,
      priority: 'HIGH',
      timestamp: new Date().toISOString(),
      metadata: {
        transactionId: request.transactionId,
        merchantName: request.merchantName,
        amount: request.transactionAmount.toString(),
        cardLastFour: request.cardLastFour,
      },
    };
  }

  formatTransactionConfirm(request: TransactionConfirmRequest): NotificationPayload {
    return {
      notificationId: uuidv4(),
      type: 'TRANSACTION_CONFIRMATION',
      customerId: request.customerId,
      subject: `Transaction Confirmed — $${request.amount.toFixed(2)} at ${request.merchantName}`,
      body: `Your transaction of $${request.amount.toFixed(2)} at ${request.merchantName} on ${request.transactionDate} has been processed successfully. Account ending ${request.accountLastFour}. Transaction ID: ${request.transactionId}.`,
      priority: 'LOW',
      timestamp: new Date().toISOString(),
      metadata: {
        transactionId: request.transactionId,
        merchantName: request.merchantName,
        amount: request.amount.toString(),
        accountLastFour: request.accountLastFour,
      },
    };
  }

  formatRegulatoryDisclosure(request: RegulatoryDisclosureRequest): NotificationPayload {
    const requiredFields = [
      'disclosureType',
      'effectiveDate',
      'regulatoryBody',
      'disclosureText',
      'referenceNumber',
    ];

    for (const field of requiredFields) {
      if (!(request as unknown as Record<string, unknown>)[field]) {
        throw new Error(`Missing required regulatory field: ${field}`);
      }
    }

    return {
      notificationId: uuidv4(),
      type: 'REGULATORY_DISCLOSURE',
      customerId: request.customerId,
      subject: `Important Regulatory Notice — ${request.disclosureType}`,
      body: `${request.disclosureText}\n\nEffective Date: ${request.effectiveDate}\nRegulatory Body: ${request.regulatoryBody}\nReference: ${request.referenceNumber}\n\n${request.acknowledgmentRequired ? 'Your acknowledgment is required. Please review and confirm.' : 'This is for your records. No action required.'}`,
      priority: 'MEDIUM',
      timestamp: new Date().toISOString(),
      metadata: {
        disclosureType: request.disclosureType,
        regulatoryBody: request.regulatoryBody,
        referenceNumber: request.referenceNumber,
        acknowledgmentRequired: request.acknowledgmentRequired.toString(),
      },
    };
  }
}
