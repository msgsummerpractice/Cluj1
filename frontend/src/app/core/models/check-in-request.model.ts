export interface CheckInRequest {
  code: string;
  method: 'QR' | 'MANUAL';
}
