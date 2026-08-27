import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { filter, switchMap } from 'rxjs/operators';
import { Event } from '../../../core/models/event.model';
import { EventService } from '../../../core/services/event.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { Sort, SortDirection } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { DataTableComponent } from '../../../shared/components/data-table/data-table';
import { DataTableCellDefDirective } from '../../../shared/components/data-table/data-table-cell-def.directive';
import { DataTableFilterDefDirective } from '../../../shared/components/data-table/data-table-filter-def.directive';
import { DataTableColumn } from '../../../shared/components/data-table/data-table.model';
import { BackButtonComponent } from '../../../shared/components/back-button/back-button';
import { EventSortField, displayEventEndDate, sortEvents } from './event-list.utils';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog.model';
import { ClearFilter } from '../../../shared/components/clear-filter/clear-filter';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';

@Component({
  selector: 'app-event-list',
  imports: [
    BackButtonComponent,
    CommonModule,
    DataTableCellDefDirective,
    DataTableComponent,
    DataTableFilterDefDirective,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIcon,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    TranslocoModule,
    ClearFilter,
    MatDatepickerModule,
    MatSelectModule,
  ],
  providers: [provideNativeDateAdapter()],
  templateUrl: './event-list.html',
  styleUrl: './event-list.css',
})
export class EventListComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly eventService = inject(EventService);
  private readonly toastService = inject(ToastService);

  readonly viewMode = computed<'ADMIN' | 'PARTICIPANT'>(() =>
    this.authService.isParticipant() ? 'PARTICIPANT' : 'ADMIN',
  );

  readonly pageTitle = computed(() =>
    this.viewMode() === 'PARTICIPANT' ? 'events.participantPageTitle' : 'events.eventPageTitle',
  );

  readonly canViewStatusColumn = computed(
    () =>
      this.authService.isAdmin() ||
      this.authService.isMarketingOrganizer() ||
      this.authService.isHrUser(),
  );

  readonly columns = computed<DataTableColumn[]>(() => {
    return [
      { key: 'name', label: 'events.eventNameColumn', sortKey: 'name' },
      { key: 'date', label: 'events.eventDateColumn', sortKey: 'startDate' },
      ...(this.canViewStatusColumn()
        ? [{ key: 'status', label: 'events.eventStatusColumn', sortKey: 'status' }]
        : []),
      { key: 'type', label: 'events.eventTypeColumn', sortKey: 'type', cellClass: 'text-gray-600' },
      {
        key: 'participantStatus',
        label: 'events.participantStatusColumn',
        headerClass: 'text-center',
        cellClass: 'text-center',
      },
      {
        key: 'actions',
        label: 'events.eventActionsColumn',
        headerClass: 'text-center',
        cellClass: 'text-center',
      },
    ];
  });

  readonly events = signal<readonly Event[]>([]);
  readonly publishingEventIds = signal<readonly string[]>([]);

  readonly dateFilterMode = signal<'range' | 'after' | 'before'>('range');

  readonly nameFilter = signal('');
  readonly dateFilterStart = signal<Date | null>(null);
  readonly dateFilterEnd = signal<Date | null>(null);
  readonly selectedStatuses = signal<readonly string[]>([]);
  readonly selectedTypes = signal<readonly string[]>([]);

  readonly sortField = signal<EventSortField | ''>('');
  readonly sortDirection = signal<SortDirection>('');

  readonly statusOptions = computed(() =>
    this.uniqueValues(this.events().map((event) => event.status)),
  );

  readonly typeOptions = computed(() => ['INTERNAL', 'EXTERNAL', 'LOCAL']);

  readonly hasActiveFilters = computed(
    () =>
      this.nameFilter().trim() !== '' ||
      this.dateFilterStart() !== null ||
      this.dateFilterEnd() !== null ||
      this.selectedStatuses().length > 0 ||
      this.selectedTypes().length > 0,
  );

  readonly filteredEvents = computed(() => {
    const nameQuery = this.nameFilter().trim().toLowerCase();
    const startDate = this.dateFilterStart();
    const endDate = this.dateFilterEnd();
    const statuses = this.selectedStatuses();
    const types = this.selectedTypes();

    return this.events().filter((event) => {
      const matchesName = event.name.toLowerCase().includes(nameQuery);
      const matchesStatus =
        statuses.length === 0 || statuses.includes(event.status.trim().toLowerCase());
      const matchesType = types.length === 0 || types.includes(event.type.trim().toLowerCase());

      const eventDate = new Date(event.startDate);
      let matchesDate = true;

      if (startDate) {
        matchesDate = matchesDate && eventDate >= startDate;
      }

      if (endDate) {
        const endOfDay = new Date(endDate);
        endOfDay.setHours(23, 59, 59, 999);
        matchesDate = matchesDate && eventDate <= endOfDay;
      }

      return matchesName && matchesStatus && matchesType && matchesDate;
    });
  });

  readonly visibleEvents = computed(() =>
    sortEvents(this.filteredEvents(), this.sortField(), this.sortDirection()),
  );

  private readonly dialog = inject(MatDialog);
  private readonly translocoService = inject(TranslocoService);

  ngOnInit(): void {
    this.fetchEvents();
  }

  handleSortChange(sort: Sort): void {
    const sortField = this.toEventSortField(sort.active);
    this.sortField.set(sortField);
    this.sortDirection.set(sortField === '' ? '' : sort.direction);
  }

  setDateFilterMode(mode: 'range' | 'after' | 'before'): void {
    this.dateFilterMode.set(mode);
    this.clearDateFilters();
  }

  setNameFilter(value: string): void {
    this.nameFilter.set(value);
  }

  setDateStart(date: Date | null): void {
    this.dateFilterStart.set(date);
  }

  setDateEnd(date: Date | null): void {
    this.dateFilterEnd.set(date);
  }

  toggleStatusFilter(status: string, checked: boolean): void {
    this.toggleFilter(this.selectedStatuses, status.trim().toLowerCase(), checked);
  }

  toggleTypeFilter(type: string, checked: boolean): void {
    this.toggleFilter(this.selectedTypes, type.trim().toLowerCase(), checked);
  }

  clearFilters(): void {
    this.nameFilter.set('');
    this.dateFilterStart.set(null);
    this.dateFilterEnd.set(null);
    this.selectedStatuses.set([]);
    this.selectedTypes.set([]);
  }

  clearNameFilters(): void {
    this.nameFilter.set('');
  }

  clearDateFilters(): void {
    this.dateFilterStart.set(null);
    this.dateFilterEnd.set(null);
  }

  clearStatusFilters(): void {
    this.selectedStatuses.set([]);
  }

  clearTypeFilters(): void {
    this.selectedTypes.set([]);
  }

  isStatusSelected(status: string): boolean {
    return this.selectedStatuses().includes(status.trim().toLowerCase());
  }

  isTypeSelected(type: string): boolean {
    return this.selectedTypes().includes(type.trim().toLowerCase());
  }

  displayEventEndDate(event: Event): boolean {
    return displayEventEndDate(event);
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

  getTypeBadgeClass(type: string): string {
    switch (type.trim().toUpperCase()) {
      case 'INTERNAL':
        return 'type-internal';
      case 'EXTERNAL':
        return 'type-external';
      case 'LOCAL':
        return 'type-local';
      default:
        return '';
    }
  }

  fetchEvents(): void {
    const fetchRequest$ =
      this.viewMode() === 'PARTICIPANT'
        ? this.eventService.getEligibleEvents()
        : this.eventService.getEvents();

    fetchRequest$.subscribe({
      next: (data) => {
        this.events.set(data);
      },
      error: (err) => {
        this.toastService.show('error', this.translocoService.translate('events.fetchError'));
      },
    });
  }

  manageEvent(eventId: string): void {
    this.router.navigate(['/events', eventId]);
  }

  isPublishing(eventId: string): boolean {
    return this.publishingEventIds().includes(eventId);
  }

  publishEvent(event: Event): void {
    if (this.isPublishing(event.id)) {
      return;
    }

    this.publishingEventIds.update((ids) => [...ids, event.id]);

    this.eventService.updateEventStatus(event.id, 'PUBLISHED').subscribe({
      next: () => {
        this.fetchEvents();
        this.clearPublishing(event.id);
        this.toastService.show(
          'success',
          this.translocoService.translate('events.publish.success'),
        );
      },
      error: (err) => {
        this.clearPublishing(event.id);
        const backendMsg: string = err?.error?.message ?? '';
        const errorKey =
          backendMsg === 'event.publish.error.startDateInPast'
            ? 'events.publish.errorStartDateInPast'
            : 'events.publish.error';
        this.toastService.show('error', this.translocoService.translate(errorKey));
      },
    });
  }

  openDialog(event: Event): void {
    if (this.isPublishing(event.id)) {
      return;
    }

    this.openConfirmDialog({
      titleKey: 'events.publish.title',
      messageKey: 'events.publish.message',
      confirmKey: 'events.publish.confirm',
      cancelKey: 'events.publish.cancel',
    })
      .pipe(
        filter((confirmed): confirmed is true => Boolean(confirmed)),
        switchMap(() => {
          this.publishingEventIds.update((ids) => [...ids, event.id]);
          return this.eventService.updateEventStatus(event.id, 'PUBLISHED');
        }),
      )
      .subscribe({
        next: () => {
          this.fetchEvents();
          this.clearPublishing(event.id);
          this.toastService.show(
            'success',
            this.translocoService.translate('events.publish.success'),
          );
        },
        error: (err) => {
          this.clearPublishing(event.id);
          const backendMsg: string = err?.error?.message ?? '';
          const errorKey =
            backendMsg === 'event.publish.error.startDateInPast'
              ? 'events.publish.errorStartDateInPast'
              : 'events.publish.error';
          this.toastService.show('error', this.translocoService.translate(errorKey));
        },
      });
  }

  checkIn(eventId: string): void {
    this.router.navigate(['/events', eventId, 'checkin']);
  }

  editEvent(eventId: string): void {
    this.router.navigate(['/events', eventId, 'edit']);
  }

  openRegistrationModal(event: any) {
    this.router.navigate(['/events', event.id, 'register']);
  }

  completeEvent(event: Event): void {
    if (!this.eventHasEnded(event)) {
      this.toastService.show('error', this.translocoService.translate('events.complete.notEnded'));
      return;
    }

    this.openConfirmDialog({
      titleKey: 'events.complete.title',
      messageKey: 'events.complete.message',
      confirmKey: 'events.complete.confirm',
      cancelKey: 'events.complete.cancel',
    })
      .pipe(
        filter((confirmed): confirmed is true => Boolean(confirmed)),
        switchMap(() => this.eventService.updateEventStatus(event.id, 'COMPLETED')),
      )
      .subscribe({
        next: () => {
          this.fetchEvents();
          this.toastService.show(
            'success',
            this.translocoService.translate('events.complete.success'),
          );
        },
        error: () => {
          this.toastService.show('error', this.translocoService.translate('events.complete.error'));
        },
      });
  }

  navigateToCheckIn(eventId: string): void {
    const event = this.events().find((e) => e.id === eventId);
    if (event?.isCheckedIn || event?.status === 'COMPLETED') {
      return;
    }
    this.router.navigate(['/events', eventId, 'checkin']);
  }

  isMarketingOrganizer(): boolean {
    return this.authService.isMarketingOrganizer();
  }

  isHrUser(): boolean {
    return this.authService.isHrUser();
  }

  isRegistrationClosed(event: Event): boolean {
    if (!event.registrationEndDate) {
      return false;
    }
    return Date.now() >= new Date(event.registrationEndDate).getTime();
  }

  eventHasEnded(event: Event): boolean {
    const currentDate = new Date();
    const eventEndDate = new Date(event.endDate);
    return currentDate > eventEndDate;
  }

  private openConfirmDialog(data: ConfirmDialogData): Observable<boolean | undefined> {
    return this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
        width: '400px',
        data,
      })
      .afterClosed();
  }

  private clearPublishing(eventId: string): void {
    this.publishingEventIds.update((ids) => ids.filter((id) => id !== eventId));
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

  manageRegistration(eventId: string) {
    this.router.navigate(['/events', eventId, 'manage']);
  }

  isPastRegistrationEndDate(event: Event): boolean {
    if (!event.registrationEndDate) {
      return false;
    }
    const currentDate = new Date();
    const registrationEndDate = new Date(event.registrationEndDate);
    return currentDate > registrationEndDate;
  }

  deleteRegistration(eventId: string): void {
    const event = this.events().find((e) => e.id === eventId);
    if (event?.isCheckedIn) {
      return;
    }

    this.openConfirmDialog({
      titleKey: 'events.deleteRegistration.title',
      messageKey: 'events.deleteRegistration.message',
      confirmKey: 'events.deleteRegistration.confirm',
      cancelKey: 'events.deleteRegistration.cancel',
    })
      .pipe(
        filter((confirmed): confirmed is true => Boolean(confirmed)),
        switchMap(() => this.eventService.deleteRegistration(eventId)),
      )
      .subscribe({
        next: () => {
          this.fetchEvents();
          this.toastService.show(
            'success',
            this.translocoService.translate('events.deleteRegistration.success'),
          );
        },
        error: () => {
          this.toastService.show(
            'error',
            this.translocoService.translate('events.deleteRegistration.error'),
          );
        },
      });
  }
}
