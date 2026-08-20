import { inject, Injectable } from '@angular/core';
import { HttpParams, HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Event } from '../models/event.model';
import { CheckInRequest } from '../models/check-in-request.model';
import { EventDetails } from '../models/event-detail.models';
import { EventRegistrationRequest } from '../models/event-registration.model';
import { AttendanceRecord } from '../models/attendance-record.model';
import { CheckInCodes } from '../models/checkincodes.model';

@Injectable({
  providedIn: 'root',
})
export class EventService {
  private apiUrl: string = 'http://localhost:8080/api/events';
  private readonly http = inject(HttpClient);

  getEvents(searchTerm?: string): Observable<Event[]> {
    let params = new HttpParams();
    if (searchTerm) {
      params = params.set('search', searchTerm);
    }
    return this.http.get<Event[]>(this.apiUrl, { params });
  }

  getEventById(id: string): Observable<Event> {
    return this.http.get<Event>(`${this.apiUrl}/${id}`);
  }

  getEventDetails(eventId: string): Observable<EventDetails> {
    const url = `${this.apiUrl}/${eventId}/details`;
    return this.http.get<EventDetails>(url);
  }

  getEventPoster(eventId: string): Observable<Blob> {
    const url = `${this.apiUrl}/${eventId}/poster`;
    return this.http.get(url, { responseType: 'blob' });
  }

  getUpcomingRegisteredEventsCountPerUser(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/countRegistrationPerUser`);
  }

  createEvent(eventData: Partial<Event>, poster?: File): Observable<Event> {
    const formData = new FormData();
    formData.append('event', new Blob([JSON.stringify(eventData)], { type: 'application/json' }));
    if (poster) {
      formData.append('poster', poster);
    }
    return this.http.post<Event>(this.apiUrl, formData, {
      withCredentials: true,
    });
  }

  updateEvent(id: string, eventData: Partial<Event>, poster?: File): Observable<Event> {
    const formData = new FormData();
    formData.append('event', new Blob([JSON.stringify(eventData)], { type: 'application/json' }));
    if (poster) {
      formData.append('poster', poster);
    }
    return this.http.put<Event>(`${this.apiUrl}/${id}`, formData, {
      withCredentials: true,
    });
  }

  getEligibleEvents(): Observable<Event[]> {
    return this.http.get<Event[]>(`${this.apiUrl}/eligible`);
  }

  registerForEvent(eventId: string, requestData: EventRegistrationRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/${eventId}`, requestData, {
      responseType: 'text',
      withCredentials: true,
    });
  }

  updateEventStatus(id: string, status: 'DRAFT' | 'PUBLISHED' | 'COMPLETED'): Observable<Event> {
    return this.http.patch<Event>(`${this.apiUrl}/${id}/status/${status}`, null, {
      withCredentials: true,
    });
  }

  checkIfAlreadyRegistered(eventId: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/${eventId}/check`, {
      withCredentials: true,
    });
  }

  generateCheckInCodes(eventId: string): Observable<CheckInCodes> {
    const url = `${this.apiUrl}/${eventId}/checkin-codes`;
    return this.http.post<CheckInCodes>(url, {}, { withCredentials: true });
  }

  getEventCheckInDetails(eventId: string): Observable<CheckInCodes> {
    return this.http.get<CheckInCodes>(`${this.apiUrl}/${eventId}/checkin`);
  }

  checkIn(request: CheckInRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/checkin`, request, {
      withCredentials: true,
    });
  }

  getRecentCheckins(eventId: string, limit: number = 4): Observable<AttendanceRecord[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<AttendanceRecord[]>(`${this.apiUrl}/${eventId}/checkins/recent`, {
      params,
    });
  }
  updateRegistration(eventId: string, requestData: EventRegistrationRequest): Observable<any> {
    return this.http.patch(`${this.apiUrl}/${eventId}/manage`, requestData, {
      withCredentials: true,
    });
  }

  deleteRegistration(eventId: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${eventId}/manage`, {
      responseType: 'text', // Backend returns a raw string here
      withCredentials: true,
    });
  }
  getRegistrationDetails(eventId: string): Observable<EventRegistrationRequest> {
    return this.http.get<EventRegistrationRequest>(`${this.apiUrl}/${eventId}/registration`, {
      withCredentials: true,
    });
  }
}
