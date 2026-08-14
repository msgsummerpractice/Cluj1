import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Event } from '../../../core/models/event.model';
import { EventDetails } from '../../../core/models/event-detail.models';
import { EventService } from '../../../core/services/event.service';
import { TranslocoModule } from '@jsverse/transloco';

@Component({
  selector: 'app-event-details',
  imports: [CommonModule, TranslocoModule],
  templateUrl: './event-details.html',
  styleUrl: './event-details.css',
})
export class EventDetailsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly eventService = inject(EventService);
  readonly event = signal<Event | null>(null);
  readonly eventDetails = signal<EventDetails | null>(null);

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
        console.error('Error fetching event:', error);
      },
    });

    this.eventService.getEventDetails(eventId).subscribe({
      next: (details) => {
        this.eventDetails.set(details);
      },
      error: (error) => {
        console.error('Error fetching event details:', error);
      },
    });
  }
}
