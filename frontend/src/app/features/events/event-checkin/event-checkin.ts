import { ActivatedRoute, Router } from '@angular/router';
import { Component, inject, OnInit, AfterViewInit, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventService } from '../../../core/services/event.service';
import { AuthService } from '../../../core/services/auth.service';
import { FormsModule } from '@angular/forms';
import { ZXingScannerModule } from '@zxing/ngx-scanner';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { BarcodeFormat } from '@zxing/library';
import { ToastService } from '../../../core/services/toast.service';
import { Platform } from '@angular/cdk/platform';
import { finalize } from 'rxjs/operators';
import { Event } from '../../../core/models/event.model';
import { CheckInRequest } from '../../../core/models/check-in-request.model';
import { AttendanceRecord } from '../../../core/models/attendance-record.model';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-event-checkin',
  standalone: true,
  imports: [CommonModule, FormsModule, ZXingScannerModule, TranslocoModule],
  templateUrl: './event-checkin.html',
  styleUrls: ['./event-checkin.css'],
})
export class EventCheckInComponent implements OnInit, AfterViewInit {
  private readonly eventService = inject(EventService);
  private readonly authService = inject(AuthService);
  private readonly translateService = inject(TranslocoService);
  private readonly toastService = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly platform = inject(Platform);
  private readonly router = inject(Router);

  readonly isMobile = this.platform.ANDROID || this.platform.IOS;
  readonly isProcessing = signal(false);
  readonly hasDevices = signal(false);
  readonly ALLOWED_FORMATS = [BarcodeFormat.QR_CODE];
  readonly scannerEnabled = signal(true);
  readonly showScanner = signal(false);

  readonly activeEvent = signal<Event | null>(null);
  readonly userTicketCode = signal<string | null>(null);
  readonly recentCheckins = signal<AttendanceRecord[]>([]);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    const eventId = this.route.snapshot.paramMap.get('id')!;
    this.loadEvent(eventId);
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.showScanner.set(true), 250);
  }

  private loadEvent(eventId: string): void {
    this.eventService.getEventById(eventId).subscribe({
      next: (event) => {
        this.activeEvent.set(event);
        this.loadUserTicket(event.id);

        if (!this.authService.isParticipant()) {
          this.loadRecentCheckins(event.id);
        }
      },
      error: (error) => {
        let errorKey = 'checkin.error.general';
        const rawError = error?.error?.message || error?.error;

        if (typeof rawError === 'string' && rawError.includes('checkin.')) {
          errorKey = rawError;
        } else if (error.status === 400) {
          errorKey = 'checkin.error.code.invalid';
        } else if (error.status === 404) {
          errorKey = 'checkin.error.event.notfound';
        } else if (error.status === 409) {
          errorKey = 'checkin.error.user.alreadycheckedin';
          this.scannerEnabled.set(false);
        }

        this.toastService.showError(this.translateService.translate(errorKey));
      },
    });
  }

  private loadUserTicket(eventId: string): void {
    this.eventService.getEventCheckInDetails(eventId).subscribe({
      next: (codes) => {
        this.userTicketCode.set(codes?.eventCode || null);
      },
      error: () => {
        this.userTicketCode.set(null);
      },
    });
  }

  private loadRecentCheckins(eventId: string): void {
    this.eventService.getRecentCheckins(eventId).subscribe({
      next: (checkins) => this.recentCheckins.set(checkins),
      error: () => {},
    });
  }

  onCamerasFound(devices: MediaDeviceInfo[]): void {
    this.hasDevices.set(!!(devices && devices.length > 0));
  }

  onCodeResult(resultString: string): void {
    if (this.isProcessing() || !resultString) return;
    const match = resultString.match(/EventID:([^|]+)/);
    const eventId = match ? match[1].trim() : resultString;
    this.processCheckIn({ eventId, method: 'QR' });
  }

  submitManual(): void {
    this.inputError.set(false);
    const code = this.manualCode();

    if (!/^\d{6}$/.test(code)) {
      this.toastService.showError(this.translateService.translate('checkin.error.code.invalid'));
      return;
    }

    this.processCheckIn({ eventCode: code, method: 'MANUAL' });
  }

  private processCheckIn(request: CheckInRequest): void {
    this.isProcessing.set(true);

    this.eventService
      .checkIn(request)
      .pipe(
        finalize(() => {
          this.isProcessing.set(false);
          if (request.method === 'MANUAL') {
            this.manualCode.set('');
          }
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.toastService.showSuccess(this.translateService.translate('checkin.success'));
          this.scannerEnabled.set(false);
          const currentEvent = this.activeEvent();
          if (currentEvent && !this.authService.isParticipant()) {
            this.loadRecentCheckins(currentEvent.id);
          }
        },
        error: (error) => {
          let errorKey = 'checkin.error.general';
          const rawError = error?.error?.message || error?.error;

          if (typeof rawError === 'string' && rawError.startsWith('checkin.')) {
            errorKey = rawError;
          } else if (error.status === 400 || error.status === 404) {
            errorKey = 'checkin.error.invalid_code';
          }

          if (error.status === 409) {
            this.scannerEnabled.set(false);
          }

          this.toastService.showError(this.translateService.translate(errorKey));
        },
      });
  }

  readonly manualCode = signal('');
  readonly inputError = signal(false);

  onManualCodeInput(value: string): void {
    const hasNonDigits = /\D/.test(value);

    if (hasNonDigits) {
      this.inputError.set(true);
    } else {
      this.inputError.set(false);
    }
    const numericValue = value.replace(/\D/g, '').slice(0, 6);
    this.manualCode.set(numericValue);
  }
}
