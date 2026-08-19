import { Component, OnDestroy, OnInit, inject, signal, computed } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Event } from '../../../core/models/event.model';
import { EventDetails } from '../../../core/models/event-detail.models';
import { EventService } from '../../../core/services/event.service';
import { TranslocoModule } from '@jsverse/transloco';
import { BackButtonComponent } from '../../../shared/components/back-button/back-button';
import { MatButtonModule } from '@angular/material/button';
import { CheckincodesComponent } from '../../checkincodes-component/checkincodes-component';
import { ToastService } from '../../../core/services/toast.service';
import { ChangeDetectorRef } from '@angular/core';
import { take } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-event-details',
  standalone: true,
  imports: [
    CommonModule,
    TranslocoModule,
    BackButtonComponent,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    CheckincodesComponent,
  ],
  templateUrl: './event-details.html',
  styleUrl: './event-details.css',
})
export class EventDetailsComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly eventService = inject(EventService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly event = signal<Event | null>(null);
  readonly eventDetails = signal<EventDetails | null>(null);
  readonly posterUrl = signal<string | null>(null);

  readonly isExporting = signal<boolean>(false);

  readonly canExport = computed(() => {
    const currentEvent = this.event();
    if (!currentEvent) return false;

    const isValidStatus =
      currentEvent.status === 'PUBLISHED' || currentEvent.status === 'COMPLETED';
    const isMarketingOrganizer = this.authService.currentUser()?.role === 'MARKETING_ORGANIZER';

    return isValidStatus && isMarketingOrganizer;
  });

  ngOnInit() {
    const eventId = this.route.snapshot.paramMap.get('id');
    if (!eventId) {
      return;
    }

    this.eventService.getEventById(eventId).subscribe({
      next: (event) => {
        this.event.set(event);
      },
      error: (error) => {
        this.toast.show('error', error);
      },
    });

    this.eventService.getEventDetails(eventId).subscribe({
      next: (details) => {
        this.eventDetails.set(details);
        if (details.hasPoster) {
          this.eventService.getEventPoster(eventId).subscribe({
            next: (poster) => {
              if (poster.size > 0) {
                this.posterUrl.set(URL.createObjectURL(poster));
              }
            },
            error: () => {
              this.posterUrl.set(null);
            },
          });
        }
      },
      error: (error) => {
        this.toast.show('error', error);
      },
    });
  }

  ngOnDestroy() {
    const url = this.posterUrl();
    if (url) {
      URL.revokeObjectURL(url);
    }
  }

  canRegister(event: Event | null): boolean {
    return event?.status === 'PUBLISHED';
  }

  isRegistrationClosed(event: Event | null): boolean {
    if (!event?.registrationEndDate) {
      return false;
    }

    return Date.now() > new Date(event.registrationEndDate).getTime();
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
