import {
  Component,
  inject,
  OnInit,
  AfterViewInit,
  PLATFORM_ID,
  ChangeDetectorRef,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { EventService } from '../../../core/services/event.service';
import { FormsModule } from '@angular/forms';
import { ZXingScannerModule } from '@zxing/ngx-scanner';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { BarcodeFormat } from '@zxing/library';
import { ToastService } from '../../../core/services/toast.service';
import { Platform } from '@angular/cdk/platform';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-event-checkin',
  standalone: true,
  imports: [FormsModule, ZXingScannerModule, TranslocoModule],
  templateUrl: './event-checkin.html',
  styleUrls: ['./event-checkin.css'],
})
export class EventCheckInComponent implements OnInit, AfterViewInit {
  private readonly eventService = inject(EventService);
  private readonly translateService = inject(TranslocoService);
  private readonly toastService = inject(ToastService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly platform = inject(Platform);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly isMobile = this.platform.ANDROID || this.platform.IOS;

  mode: 'SCAN' | 'MANUAL' = this.isMobile ? 'SCAN' : 'MANUAL';
  manualCode: string = '';
  isProcessing: boolean = false;
  hasDevices: boolean = false;
  isLoading: boolean = false;

  eventId: string = '';
  qrCodeContent: string | null = null;

  isScannerReady: boolean = false;
  allowedFormats = [BarcodeFormat.QR_CODE];

  ngOnInit(): void {
    this.eventId = this.route.snapshot.paramMap.get('id') ?? '';
    if (this.eventId) {
      this.loadCheckInDetails();
    }
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => {
        this.isScannerReady = true;
      }, 100);
    }
  }

  private loadCheckInDetails(): void {
    this.isLoading = true;
    this.eventService.getEventCheckInDetails(this.eventId).subscribe({
      next: (details) => {
        this.qrCodeContent = details.qrCodeContent;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isLoading = false;
        this.showError('checkin.error.not_available');
        this.router.navigate(['/events']);
      },
    });
  }

  toggleMode(): void {
    if (this.isMobile) {
      this.mode = this.mode === 'SCAN' ? 'MANUAL' : 'SCAN';
    }
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
    this.cdr.markForCheck();

    this.eventService
      .checkIn({ code, method })
      .pipe(
        finalize(() => {
          this.isProcessing = false;
          if (method === 'MANUAL') {
            this.manualCode = '';
          }
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: () => {
          this.showSuccess('checkin.success');
          if (this.eventId) {
            this.router.navigate(['/events']);
          }
        },
        error: (error) => {
          let errorKey = 'checkin.error.general';

          if (error.status === 400 || error.status === 404) {
            errorKey = 'checkin.error.invalid_code';
          } else {
            const rawError = error?.error?.message || error?.error;
            if (typeof rawError === 'string' && !rawError.includes(' ')) {
              errorKey = rawError;
            }
          }

          this.showError(errorKey);
        },
      });
  }

  private showSuccess(key: string): void {
    const translatedMessage = this.translateService.translate(key);
    this.toastService.show('success', translatedMessage);
  }

  private showError(key: string): void {
    const translatedMessage = this.translateService.translate(key);
    this.toastService.show('error', translatedMessage);
  }
}
