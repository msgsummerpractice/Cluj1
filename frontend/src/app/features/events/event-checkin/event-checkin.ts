import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Component, inject, OnInit, signal, AfterViewInit, PLATFORM_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventService } from '../../../core/services/event.service';
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

@Component({
  selector: 'app-event-checkin',
  standalone: true,
  imports: [CommonModule, FormsModule, ZXingScannerModule, TranslocoModule],
  templateUrl: './event-checkin.html',
  styleUrls: ['./event-checkin.css'],
})
export class EventCheckInComponent implements OnInit, AfterViewInit {
  private readonly eventService = inject(EventService);
  private readonly translateService = inject(TranslocoService);
  private readonly toastService = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly platform = inject(Platform);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);

  readonly isMobile = this.platform.ANDROID || this.platform.IOS;

  manualCode: string = '';
  isLoading: boolean = false;
  isScannerReady: boolean = false;

  eventId: string = '';
  readonly isProcessing = signal(false);
  readonly hasDevices = signal(false);
  readonly ALLOWED_FORMATS = [BarcodeFormat.QR_CODE];
  readonly scannerEnabled = signal(true);

  readonly activeEvent = signal<Event | null>(null);
  readonly userTicketCode = signal<string | null>(null);
  readonly recentCheckins = signal<AttendanceRecord[]>([]);

  ngOnInit(): void {
    const eventId = this.route.snapshot.paramMap.get('id')!;
    this.loadEvent(eventId);
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => {
        this.isScannerReady = true;
      }, 100);
    }
  }

  private loadEvent(eventId: string): void {
    this.eventService.getEventById(eventId).subscribe({
      next: (event) => {
        this.activeEvent.set(event);
        this.loadUserTicket(event.id);
        this.loadRecentCheckins(event.id);
      },
      error: () =>
        this.toastService.showError(
          this.translateService.translate('checkin.error.event.notfound'),
        ),
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
    this.processCheckIn({ eventId: resultString, method: 'QR' });
  }

  submitManual(): void {
    if (this.manualCode.length !== 6) {
      this.toastService.showError(this.translateService.translate('checkin.error.code.invalid'));
      return;
    }
    this.processCheckIn({ eventCode: this.manualCode, method: 'MANUAL' });
  }

  private processCheckIn(request: CheckInRequest): void {
    this.isProcessing.set(true);

    this.eventService
      .checkIn(request)
      .pipe(
        finalize(() => {
          this.isProcessing.set(false);
          if (request.method === 'MANUAL') {
            this.manualCode = '';
          }
        }),
      )
      .subscribe({
        next: () => {
          this.toastService.showSuccess(this.translateService.translate('checkin.success'));
          this.scannerEnabled.set(false);
          const currentEvent = this.activeEvent();
          if (currentEvent) {
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
}
