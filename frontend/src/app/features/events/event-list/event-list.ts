import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Event } from '../../../core/models/event.model';
import { EventService } from '../../../core/services/event.service';

import { Sort, SortDirection } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { TranslocoModule } from '@jsverse/transloco';
import { DataTableComponent } from '../../../shared/components/data-table/data-table';
import { DataTableCellDefDirective } from '../../../shared/components/data-table/data-table-cell-def.directive';
import { DataTableFilterDefDirective } from '../../../shared/components/data-table/data-table-filter-def.directive';
import { DataTableColumn } from '../../../shared/components/data-table/data-table.model';
import { EventSortField, shouldShowEventEndDate, sortEvents } from './event-list.utils';

@Component({
  selector: 'app-event-list',
  imports: [
    CommonModule,
    MatButtonModule,
    MatCheckboxModule,
    MatInputModule,
    MatFormFieldModule,
    TranslocoModule,
    DataTableComponent,
    DataTableCellDefDirective,
    DataTableFilterDefDirective,
  ],
  templateUrl: './event-list.html',
  styleUrl: './event-list.css',
})
export class EventListComponent implements OnInit {
  readonly columns: readonly DataTableColumn[] = [
    { key: 'name', label: 'events.eventNameColumn', sortKey: 'name' },
    { key: 'date', label: 'events.eventDateColumn', sortKey: 'startDate' },
    { key: 'status', label: 'events.eventStatusColumn', sortKey: 'status' },
    { key: 'type', label: 'events.eventTypeColumn', sortKey: 'type', cellClass: 'text-gray-600' },
    {
      key: 'actions',
      label: 'events.eventActionsColumn',
      headerClass: 'text-center',
      cellClass: 'text-center',
    },
  ];
  readonly events = signal<readonly Event[]>([]);
  readonly nameFilter = signal('');
  readonly selectedDateYears = signal<readonly string[]>([]);
  readonly selectedDateMonths = signal<readonly string[]>([]);
  readonly selectedStatuses = signal<readonly string[]>([]);
  readonly selectedTypes = signal<readonly string[]>([]);
  readonly sortField = signal<EventSortField | ''>('');
  readonly sortDirection = signal<SortDirection>('');
  readonly dateYearOptions = computed(() =>
    this.uniqueValues(
      this.events()
        .map((event) => this.getDateParts(event.startDate).year)
        .filter((year) => year !== ''),
    ),
  );
  readonly dateMonthOptions = computed(() =>
    this.uniqueValues(
      this.events()
        .map((event) => this.getDateParts(event.startDate).month)
        .filter((month) => month !== ''),
    ).sort((firstMonth, secondMonth) => Number(firstMonth) - Number(secondMonth)),
  );
  readonly statusOptions = computed(() =>
    this.uniqueValues(this.events().map((event) => event.status)),
  );
  readonly typeOptions = computed(() =>
    this.uniqueValues(this.events().map((event) => event.type)),
  );
  readonly hasActiveFilters = computed(
    () =>
      this.nameFilter().trim() !== '' ||
      this.selectedDateYears().length > 0 ||
      this.selectedDateMonths().length > 0 ||
      this.selectedStatuses().length > 0 ||
      this.selectedTypes().length > 0,
  );
  readonly filteredEvents = computed(() => {
    const nameQuery = this.nameFilter().trim().toLowerCase();
    const dateYears = this.selectedDateYears();
    const dateMonths = this.selectedDateMonths();
    const statuses = this.selectedStatuses();
    const types = this.selectedTypes();

    return this.events().filter((event) => {
      const matchesName = event.name.toLowerCase().includes(nameQuery);
      const dateParts = this.getDateParts(event.startDate);
      const matchesDateYear = dateYears.length === 0 || dateYears.includes(dateParts.year);
      const matchesDateMonth = dateMonths.length === 0 || dateMonths.includes(dateParts.month);
      const matchesStatus =
        statuses.length === 0 || statuses.includes(event.status.trim().toLowerCase());
      const matchesType = types.length === 0 || types.includes(event.type.trim().toLowerCase());

      return matchesName && matchesDateYear && matchesDateMonth && matchesStatus && matchesType;
    });
  });
  readonly visibleEvents = computed(() =>
    sortEvents(this.filteredEvents(), this.sortField(), this.sortDirection()),
  );

  private readonly eventService = inject(EventService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.fetchEvents();
  }

  handleSortChange(sort: Sort): void {
    const sortField = this.toEventSortField(sort.active);

    this.sortField.set(sortField);
    this.sortDirection.set(sortField === '' ? '' : sort.direction);
  }

  setNameFilter(value: string): void {
    this.nameFilter.set(value);
  }

  toggleDateYearFilter(year: string, checked: boolean): void {
    this.toggleFilter(this.selectedDateYears, year, checked);
  }

  toggleDateMonthFilter(month: string, checked: boolean): void {
    this.toggleFilter(this.selectedDateMonths, month, checked);
  }

  toggleStatusFilter(status: string, checked: boolean): void {
    this.toggleFilter(this.selectedStatuses, status.trim().toLowerCase(), checked);
  }

  toggleTypeFilter(type: string, checked: boolean): void {
    this.toggleFilter(this.selectedTypes, type.trim().toLowerCase(), checked);
  }

  clearFilters(): void {
    this.nameFilter.set('');
    this.selectedDateYears.set([]);
    this.selectedDateMonths.set([]);
    this.selectedStatuses.set([]);
    this.selectedTypes.set([]);
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

  isDateYearSelected(year: string): boolean {
    return this.selectedDateYears().includes(year);
  }

  isDateMonthSelected(month: string): boolean {
    return this.selectedDateMonths().includes(month);
  }

  getMonthLabel(month: string): Date {
    return new Date(2000, Number(month), 1);
  }

  isStatusSelected(status: string): boolean {
    return this.selectedStatuses().includes(status.trim().toLowerCase());
  }

  isTypeSelected(type: string): boolean {
    return this.selectedTypes().includes(type.trim().toLowerCase());
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

  private uniqueValues(values: readonly string[]): string[] {
    const unique = new Map<string, string>();

    values.forEach((value) => {
      const key = value.trim().toLowerCase();
      if (!unique.has(key)) {
        unique.set(key, value);
      }
    });

    return [...unique.values()];
  }

  private getDateParts(value: string): { year: string; month: string } {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return { year: '', month: '' };
    }

    return {
      year: String(date.getFullYear()),
      month: String(date.getMonth()),
    };
  }

  private toggleFilter(
    filter: ReturnType<typeof signal<readonly string[]>>,
    value: string,
    checked: boolean,
  ): void {
    const currentValues = filter();
    filter.set(
      checked
        ? currentValues.includes(value)
          ? currentValues
          : [...currentValues, value]
        : currentValues.filter((currentValue) => currentValue !== value),
    );
  }
}
