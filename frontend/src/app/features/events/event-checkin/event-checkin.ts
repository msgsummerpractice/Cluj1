import { Component, inject } from '@angular/core';
import { EventCheckInService } from './event-checkin.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ZXingScannerModule } from '@zxing/ngx-scanner';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { BarcodeFormat } from '@zxing/library';

@Component({
  selector: 'app-event-checkin',
  standalone: true,
  imports: [CommonModule, FormsModule, ZXingScannerModule, TranslatePipe],
  templateUrl: './event-checkin.html',
  styleUrls: ['./event-checkin.css'],
})
export class EventCheckInComponent {
  private readonly eventCheckInService = inject(EventCheckInService);
  private translateService = inject(TranslateService);
  // private toastService = inject(ToastService);

  mode: 'SCAN' | 'MANUAL' = 'SCAN';
  manualCode: string = '';
  isProcessing: boolean = false;
  hasDevices: boolean = false;
  allowedFormats = [BarcodeFormat.QR_CODE];

  toggleMode(): void {
    this.mode = this.mode === 'SCAN' ? 'MANUAL' : 'SCAN';
    this.manualCode = '';
  }

  onCamerasFound(devices: MediaDeviceInfo[]): void {
    this.hasDevices = devices && devices.length > 0;
  }

  onCodeResult(resultString: string): void {
    if (this.isProcessing) return;
    this.processCheckIn(resultString, 'QR');
  }

  submitManual(): void {
    if (this.manualCode.length !== 6) {
      this.showError('checkin.error.code.invalid');
      return;
    }

    this.processCheckIn(this.manualCode, 'MANUAL');
  }

  private processCheckIn(code: string, method: 'QR' | 'MANUAL'): void {
    this.isProcessing = true;

    this.eventCheckInService.checkIn({ code, method }).subscribe({
      next: () => {
        this.showSuccess('checkin.success');
        this.isProcessing = false;
        if (method === 'MANUAL') {
          this.manualCode = '';
        }
      },
      error: (error) => {
        const errorKey = error.error?.message || 'checkin.error.general';
        this.showError(errorKey);
        this.isProcessing = false;
        if (method === 'MANUAL') {
          this.manualCode = '';
        }
      },
    });
  }

  private showSuccess(key: string): void {
    const translatedMessage = this.translateService.instant(key);
    // this.toastService.showSuccess(translatedMessage);
    console.log('Success:', translatedMessage);
  }

  private showError(key: string): void {
    const translatedMessage = this.translateService.instant(key);
    // this.toastService.showError(translatedMessage);
    console.error('Error:', translatedMessage);
  }
}
