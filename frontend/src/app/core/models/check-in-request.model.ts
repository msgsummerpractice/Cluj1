export interface CheckInRequest {
  eventId?: string;
  eventCode?: string;
  method: 'QR' | 'MANUAL';
}
