import { inject, Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Event } from '../models/event.model';
import { EventDetails } from '../models/event-detail.models';
import { EventRegistrationRequest } from '../models/event-registration.model';

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

  registerForEvent(eventId: string, requestData: EventRegistrationRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/${eventId}`, requestData, {
      responseType: 'text',
      withCredentials: true,
    });
  }

  checkIfAlreadyRegistered(eventId: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/${eventId}/check`, {
      withCredentials: true,
    });
  }
}
