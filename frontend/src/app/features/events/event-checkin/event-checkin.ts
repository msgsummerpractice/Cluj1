import { Component, inject, AfterViewInit, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { EventService } from '../../../core/services/event.service';
import { FormsModule } from '@angular/forms';
import { ZXingScannerModule } from '@zxing/ngx-scanner';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { BarcodeFormat } from '@zxing/library';

@Component({
  selector: 'app-event-checkin',
  standalone: true,
  imports: [FormsModule, ZXingScannerModule, TranslocoModule],
  templateUrl: './event-checkin.html',
  styleUrls: ['./event-checkin.css'],
})
export class EventCheckInComponent implements AfterViewInit {
  private readonly eventService = inject(EventService);
  private translateService = inject(TranslocoService);
  private platformId = inject(PLATFORM_ID);

  mode: 'SCAN' | 'MANUAL' = 'SCAN';
  manualCode: string = '';
  isProcessing: boolean = false;
  hasDevices: boolean = false;

  isScannerReady: boolean = false;
  allowedFormats = [BarcodeFormat.QR_CODE];

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => {
        this.isScannerReady = true;
      }, 100);
    }
  }

  toggleMode(): void {
    this.mode = this.mode === 'SCAN' ? 'MANUAL' : 'SCAN';
    this.manualCode = '';
  }

  onCamerasFound(devices: MediaDeviceInfo[]): void {
    this.hasDevices = devices && devices.length > 0;
  }

  onCodeResult(resultString: string): void {
    if (this.isProcessing || !resultString) return;
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

    this.eventService.checkIn({ code, method }).subscribe({
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
    const translatedMessage = this.translateService.translate(key);
    console.log('Success:', translatedMessage);
  }

  private showError(key: string): void {
    const translatedMessage = this.translateService.translate(key);
    console.error('Error:', translatedMessage);
  }
}
