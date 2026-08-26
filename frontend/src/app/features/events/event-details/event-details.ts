import { Component, effect, inject, signal, computed } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import DOMPurify from 'dompurify';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Event } from '../../../core/models/event.model';
import { EventDetails } from '../../../core/models/event-detail.models';
import { EventService } from '../../../core/services/event.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CheckincodesComponent } from '../../checkincodes-component/checkincodes-component';
import { ToastService } from '../../../core/services/toast.service';
import { finalize, take } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { BackButtonComponent } from '../../../shared/components/back-button/back-button';

@Component({
  selector: 'app-event-details',
  standalone: true,
  imports: [
    CommonModule,
    TranslocoModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    CheckincodesComponent,
    BackButtonComponent,
  ],
  templateUrl: './event-details.html',
  styleUrl: './event-details.css',
})
export class EventDetailsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly eventService = inject(EventService);
  private readonly authService = inject(AuthService);
  private readonly translocoService = inject(TranslocoService);
  private readonly toast = inject(ToastService);
  private readonly sanitizer = inject(DomSanitizer);

  private readonly eventId = signal(this.route.snapshot.paramMap.get('id') ?? undefined);

  private readonly eventResource = rxResource({
    params: () => this.eventId(),
    stream: ({ params: id }) => this.eventService.getEventById(id),
  });

  private readonly validEventId = computed(() =>
    this.eventResource.hasValue() ? this.eventId() : undefined,
  );

  private readonly registrationResource = rxResource({
    params: () => this.validEventId(),
    stream: ({ params: id }) => this.eventService.checkIfAlreadyRegistered(id),
    defaultValue: false,
  });

  private readonly detailsResource = rxResource({
    params: () => this.validEventId(),
    stream: ({ params: id }) => this.eventService.getEventDetails(id),
  });

  private readonly posterResource = rxResource({
    params: () =>
      this.detailsResource.hasValue() && this.detailsResource.value()?.hasPoster
        ? this.validEventId()
        : undefined,
    stream: ({ params: id }) => this.eventService.getEventPoster(id),
  });

  readonly event = computed<Event | null>(() =>
    this.eventResource.hasValue() ? this.eventResource.value() : null,
  );
  readonly eventDetails = computed<EventDetails | null>(() =>
    this.detailsResource.hasValue() ? this.detailsResource.value() : null,
  );
  readonly isRegistered = computed(() =>
    this.registrationResource.hasValue() ? this.registrationResource.value() : false,
  );
  readonly posterUrl = signal<string | null>(null);

  readonly sanitizedDescription = computed<SafeHtml | null>(() => {
    const description = this.eventDetails()?.description;
    if (!description) return null;
    return this.sanitizer.bypassSecurityTrustHtml(DOMPurify.sanitize(description));
  });

  readonly isExporting = signal<boolean>(false);
  readonly isDownloadingReport = signal<boolean>(false);

  readonly canExport = computed(() => {
    const currentEvent = this.event();
    if (!currentEvent) return false;

    const isValidStatus =
      currentEvent.status === 'PUBLISHED' || currentEvent.status === 'COMPLETED';
    const isMarketingOrganizer = this.authService.currentUser()?.role === 'MARKETING_ORGANIZER';

    return isValidStatus && isMarketingOrganizer;
  });

  readonly canDownloadReport = computed(() => {
    const currentEvent = this.event();
    return currentEvent?.status === 'COMPLETED' && this.authService.isHrUser();
  });

  readonly showCheckInCodes = computed(() => {
    const currentEvent = this.event();
    return (
      !!currentEvent &&
      currentEvent.status === 'PUBLISHED' &&
      this.authService.isMarketingOrganizer()
    );
  });

  readonly showActionsPanel = computed(() => {
    const currentEvent = this.event();
    if (!currentEvent) return false;

    return (
      this.canRegister(currentEvent) ||
      this.canCheckIn() ||
      this.canExport() ||
      this.canDownloadReport() ||
      ((currentEvent.status === 'PUBLISHED' || currentEvent.status === 'COMPLETED') &&
        this.canViewStatistics())
    );
  });

  readonly canCheckIn = computed(() => {
    const currentEvent = this.event();
    if (!currentEvent) return false;
    return currentEvent.status !== 'DRAFT' && this.isRegistered();
  });

  constructor() {
    effect((onCleanup) => {
      const poster = this.posterResource.value();
      if (!poster || poster.size === 0) {
        this.posterUrl.set(null);
        return;
      }

      const url = URL.createObjectURL(poster);
      this.posterUrl.set(url);
      onCleanup(() => URL.revokeObjectURL(url));
    });

    effect(() => {
      if (this.eventResource.error()) {
        this.router.navigate(['/not-found']);
      }
    });

    effect(() => {
      const error = this.detailsResource.error() as { message?: string } | undefined;
      if (error && this.eventResource.hasValue()) {
        this.toast.show('error', error.message ?? String(error));
      }
    });
  }

  canRegister(event: Event | null): boolean {
    return event?.status === 'PUBLISHED' && !this.isRegistered();
  }

  isRegistrationClosed(event: Event | null): boolean {
    if (!event?.registrationEndDate) {
      return false;
    }

    return Date.now() > new Date(event.registrationEndDate).getTime();
  }

  canViewStatistics(): boolean {
    return this.authService.isMarketingOrganizer();
  }

  onDownloadAttendanceReport(): void {
    const currentEvent = this.event();
    if (!currentEvent || !this.canDownloadReport() || this.isDownloadingReport()) return;

    this.isDownloadingReport.set(true);

    this.eventService
      .downloadAttendanceReport(currentEvent.id)
      .pipe(finalize(() => this.isDownloadingReport.set(false)))
      .subscribe({
        next: (report) => {
          const reportUrl = URL.createObjectURL(report);
          const downloadLink = document.createElement('a');
          downloadLink.href = reportUrl;
          downloadLink.download = `attendance-report-${currentEvent.id}.xlsx`;
          downloadLink.click();
          URL.revokeObjectURL(reportUrl);
        },
        error: () => {
          this.toast.show('error', this.translocoService.translate('events.reportError'));
        },
      });
  }

  onExportExcel(): void {
    const currentEvent = this.event();
    if (!currentEvent || !this.canExport()) return;

    this.isExporting.set(true);

    this.eventService
      .exportEventData(currentEvent.id)
      .pipe(take(1))
      .subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `attendance_report_${currentEvent.name.replace(/\s+/g, '_')}.xlsx`;
          document.body.appendChild(a);
          a.click();
          a.remove();
          window.URL.revokeObjectURL(url);

          this.isExporting.set(false);
        },
        error: (error) => {
          this.toast.show('error', error);
          this.isExporting.set(false);
        },
      });
  }
}
