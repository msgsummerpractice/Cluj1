import { Component, OnDestroy, inject, signal, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Event } from '../../../core/models/event.model';
import { EventDetails } from '../../../core/models/event-detail.models';
import { EventService } from '../../../core/services/event.service';
import { TranslocoModule } from '@jsverse/transloco';
import { BackButtonComponent } from '../../../shared/components/back-button/back-button';
import { MatButtonModule } from '@angular/material/button';
import {CheckincodesComponent} from '../../checkincodes-component/checkincodes-component';
import { ToastService } from '../../../core/services/toast.service';
import{ChangeDetectorRef} from '@angular/core';

@Component({
  selector: 'app-event-details',
  imports: [
    CommonModule,
    TranslocoModule,
    BackButtonComponent,
    RouterLink,
    MatButtonModule,
    CheckincodesComponent,
  ],
  templateUrl: './event-details.html',
  styleUrl: './event-details.css',
})
export class EventDetailsComponent implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly eventService = inject(EventService);
  readonly event = signal<Event | null>(null);
  readonly eventDetails = signal<EventDetails | null>(null);
  readonly posterUrl = signal<string | null>(null);

  private toast = inject(ToastService);
  private cdr = inject(ChangeDetectorRef);

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
}
