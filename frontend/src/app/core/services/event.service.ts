import { inject, Injectable } from '@angular/core';
import { HttpParams, HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Event } from '../models/event.model';
import { CheckInRequest } from '../models/check-in-request.model';
import { EventDetails } from '../models/event-detail.models';
import { EventRegistrationRequest } from '../models/event-registration.model';
import { AttendanceRecord } from '../models/attendance-record.model';
import { CheckInCodes } from '../models/checkincodes.model';
import { EventStatistics } from '../models/event-statistics.model';
import { environment } from '../../../environments/environment.prod';

@Injectable({
  providedIn: 'root',
})
export class EventService {
  // private apiUrl: string = 'http://localhost:8080/api/events';
  private baseUrl: string = environment.apiUrl;
  private readonly http = inject(HttpClient);

  getEvents(searchTerm?: string): Observable<Event[]> {
    let params = new HttpParams();
    if (searchTerm) {
      params = params.set('search', searchTerm);
    }
    return this.http.get<Event[]>(`${this.baseUrl}/api/events`, { params });
  }

  getEventById(id: string): Observable<Event> {
    return this.http.get<Event>(`${this.baseUrl}/api/events/${id}`);
  }

  getEventDetails(eventId: string): Observable<EventDetails> {
    const url = `${this.baseUrl}/api/events/${eventId}/details`;
    return this.http.get<EventDetails>(url);
  }

  getEventPoster(eventId: string): Observable<Blob> {
    const url = `${this.baseUrl}/api/events/${eventId}/poster`;
    return this.http.get(url, { responseType: 'blob' });
  }

  getUpcomingRegisteredEventsCountPerUser(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/api/events/countRegistrationPerUser`);
  }

  createEvent(eventData: Partial<Event>, poster?: File): Observable<Event> {
    const formData = new FormData();
    formData.append('event', new Blob([JSON.stringify(eventData)], { type: 'application/json' }));
    if (poster) {
      formData.append('poster', poster);
    }
    return this.http.post<Event>(`${this.baseUrl}/api/events`, formData, {
      withCredentials: true,
    });
  }

  updateEvent(id: string, eventData: Partial<Event>, poster?: File): Observable<Event> {
    const formData = new FormData();
    formData.append('event', new Blob([JSON.stringify(eventData)], { type: 'application/json' }));
    if (poster) {
      formData.append('poster', poster);
    }
    return this.http.put<Event>(`${this.baseUrl}/api/events/${id}`, formData, {
      withCredentials: true,
    });
  }

  getEligibleEvents(): Observable<Event[]> {
    return this.http.get<Event[]>(`${this.baseUrl}/api/events/eligible`);
  }

  registerForEvent(eventId: string, requestData: EventRegistrationRequest): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/events/${eventId}`, requestData, {
      responseType: 'text',
      withCredentials: true,
    });
  }

  updateEventStatus(id: string, status: 'DRAFT' | 'PUBLISHED' | 'COMPLETED'): Observable<Event> {
    return this.http.patch<Event>(`${this.baseUrl}/api/events/${id}/status/${status}`, null, {
      withCredentials: true,
    });
  }

  checkIfAlreadyRegistered(eventId: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/api/events/${eventId}/check`, {
      withCredentials: true,
    });
  }

  generateCheckInCodes(eventId: string): Observable<CheckInCodes> {
    const url = `${this.baseUrl}/api/events/${eventId}/checkin-codes`;
    return this.http.post<CheckInCodes>(url, {}, { withCredentials: true });
  }

  getEventCheckInDetails(eventId: string): Observable<CheckInCodes> {
    return this.http.get<CheckInCodes>(`${this.baseUrl}/api/events/${eventId}/checkin`);
  }

  checkIn(request: CheckInRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/api/events/checkin`, request, {
      withCredentials: true,
    });
  }

  getRecentCheckins(eventId: string, limit: number = 4): Observable<AttendanceRecord[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<AttendanceRecord[]>(`${this.baseUrl}/api/events/${eventId}/checkins/recent`, {
      params,
    });
  }

  getEventStatistics(eventId: string): Observable<EventStatistics> {
    return this.http.get<EventStatistics>(`${this.baseUrl}/api/events/${eventId}/statistics`, {
      withCredentials: true,
    });
  }

  exportEventData(eventId: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/api/events/${eventId}/export`, {
      responseType: 'blob',
      withCredentials: true,
    });
  }

  downloadAttendanceReport(eventId: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/api/events/${eventId}/attendance-report`, {
      responseType: 'blob',
      withCredentials: true,
    });
  }
  updateRegistration(eventId: string, requestData: EventRegistrationRequest): Observable<any> {
    return this.http.patch(`${this.baseUrl}/api/events/${eventId}/manage`, requestData, {
      withCredentials: true,
    });
  }

  deleteRegistration(eventId: string): Observable<any> {
    return this.http.delete(`${this.baseUrl}/api/events/${eventId}/manage`, {
      responseType: 'text',
      withCredentials: true,
    });
  }
  getRegistrationDetails(eventId: string): Observable<EventRegistrationRequest> {
    return this.http.get<EventRegistrationRequest>(`${this.baseUrl}/api/events/${eventId}/registration`, {
      withCredentials: true,
    });
  }
}
