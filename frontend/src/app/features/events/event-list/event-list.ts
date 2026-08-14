import {
  AfterViewInit,
  Component,
  computed,
  effect,
  inject,
  OnInit,
  signal,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Event } from '../../../core/models/event.model';
import { EventService } from '../../../core/services/event.service';

import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatSort, MatSortModule, Sort, SortDirection } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { TranslocoModule } from '@jsverse/transloco';
import { EventSortField, shouldShowEventEndDate, sortEvents } from './event-list.utils';

@Component({
  selector: 'app-event-list',
  imports: [
    CommonModule,
    MatTableModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    TranslocoModule,
  ],
  templateUrl: './event-list.html',
  styleUrl: './event-list.css',
})
export class EventListComponent implements OnInit, AfterViewInit {
  @ViewChild(MatSort) private sort?: MatSort;

  readonly displayedColumns: string[] = ['name', 'date', 'status', 'type', 'actions'];
  readonly dataSource = new MatTableDataSource<Event>([]);
  readonly events = signal<readonly Event[]>([]);
  readonly sortField = signal<EventSortField | ''>('');
  readonly sortDirection = signal<SortDirection>('');
  readonly visibleEvents = computed(() =>
    sortEvents(this.events(), this.sortField(), this.sortDirection()),
  );

  private readonly eventService = inject(EventService);
  private readonly router = inject(Router);

  private readonly syncDataSource = effect(() => {
    this.dataSource.data = this.visibleEvents();
  });

  ngOnInit(): void {
    this.fetchEvents();
  }

  ngAfterViewInit(): void {
    if (this.sort !== undefined) {
      this.dataSource.sort = this.sort;
    }
  }

  handleSortChange(sort: Sort): void {
    const sortField = this.toEventSortField(sort.active);

    this.sortField.set(sortField);
    this.sortDirection.set(sortField === '' ? '' : sort.direction);
  }

  shouldShowEndDate(event: Event): boolean {
    return shouldShowEventEndDate(event);
  }

  getStatusBadgeClass(status: string): string {
    const normalizedStatus = status.trim().toLowerCase();

    if (normalizedStatus.includes('draft')) {
      return 'status-draft';
    }

    if (normalizedStatus.includes('published')) {
      return 'status-published';
    }

    return 'status-completed';
  }

  fetchEvents(): void {
    this.eventService.getEvents().subscribe({
      next: (data) => {
        this.events.set(data);
      },
      error: (err) => console.error('Error fetching events', err),
    });
  }

  manageEvent(eventId: string): void {
    this.router.navigate(['/admin/events', eventId, 'manage']);
  }

  private toEventSortField(sortField: string): EventSortField | '' {
    switch (sortField) {
      case 'name':
      case 'startDate':
      case 'type':
      case 'status':
        return sortField;
      default:
        return '';
    }
  }
}
