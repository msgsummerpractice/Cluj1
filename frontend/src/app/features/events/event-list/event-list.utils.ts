import { SortDirection } from '@angular/material/sort';
import { Event } from '../../../core/models/event.model';

export type EventSortField = 'name' | 'startDate' | 'type' | 'status';

export function sortEvents(
  events: readonly Event[],
  sortField: EventSortField | '',
  sortDirection: SortDirection,
): Event[] {
  if (sortField === '' || sortDirection === '') {
    return [...events];
  }

  const directionMultiplier = sortDirection === 'asc' ? 1 : -1;

  return [...events].sort(
    (firstEvent, secondEvent) =>
      compareEvents(firstEvent, secondEvent, sortField) * directionMultiplier,
  );
}

export function shouldShowEventEndDate(event: Event): boolean {
  const startDateValue = new Date(event.startDate);
  const endDateValue = new Date(event.endDate);

  if (Number.isNaN(startDateValue.getTime()) || Number.isNaN(endDateValue.getTime())) {
    return event.startDate !== event.endDate;
  }

  return startDateValue.toDateString() !== endDateValue.toDateString();
}

function compareEvents(firstEvent: Event, secondEvent: Event, sortField: EventSortField): number {
  if (sortField === 'startDate') {
    return compareDateValues(firstEvent.startDate, secondEvent.startDate);
  }

  return firstEvent[sortField].localeCompare(secondEvent[sortField]);
}

function compareDateValues(firstDate: string, secondDate: string): number {
  const firstTime = new Date(firstDate).getTime();
  const secondTime = new Date(secondDate).getTime();

  if (!Number.isNaN(firstTime) && !Number.isNaN(secondTime)) {
    return firstTime - secondTime;
  }

  return firstDate.localeCompare(secondDate);
}
