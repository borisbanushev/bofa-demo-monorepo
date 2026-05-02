export interface FraudAlertRequest {
  customerId: string;
  transactionId: string;
  transactionAmount: number;
  merchantName: string;
  transactionDate: string;
  cardLastFour: string;
}

export interface TransactionConfirmRequest {
  customerId: string;
  transactionId: string;
  amount: number;
  merchantName: string;
  transactionDate: string;
  accountLastFour: string;
}

export interface RegulatoryDisclosureRequest {
  customerId: string;
  disclosureType: string;
  effectiveDate: string;
  regulatoryBody: string;
  disclosureText: string;
  acknowledgmentRequired: boolean;
  referenceNumber: string;
}

export interface NotificationPayload {
  notificationId: string;
  type: string;
  customerId: string;
  subject: string;
  body: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  timestamp: string;
  metadata: Record<string, string>;
}
