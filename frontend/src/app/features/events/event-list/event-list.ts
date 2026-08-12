import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { Event } from '../../../core/models/event.model';
import { EventService } from '../../../core/services/event.service';

import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';

@Component({
  selector: 'app-event-list',
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatInputModule,
    MatFormFieldModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
  ],
  templateUrl: './event-list.html',
  styleUrl: './event-list.css',
})
export class EventListComponent implements OnInit {
  events: Event[] = [];
  displayedColumns: string[] = ['name', 'date', 'type', 'status', 'actions'];
  searchTerm: string = '';
  searchSubject: Subject<string> = new Subject<string>();

  constructor(
    private eventService: EventService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.fetchEvents();

    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe((term) => {
      this.fetchEvents(term);
    });
  }

  onSearch(term: string): void {
    this.searchSubject.next(term);
  }

  fetchEvents(search?: string): void {
    this.eventService.getEvents(search).subscribe({
      next: (data) => {
        this.events = data;
      },
      error: (err) => console.error('Error fetching events', err),
    });
  }

  manageEvent(eventId: string): void {
    this.router.navigate(['/admin/events', eventId, 'manage']);
  }
}
