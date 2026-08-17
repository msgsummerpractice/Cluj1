import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Event } from '../models/event.model';
import { EventDetails } from '../models/event-detail.models';

@Injectable({
  providedIn: 'root',
})
export class EventService {
  private apiUrl = 'http://localhost:8080/api/events';

  constructor(private http: HttpClient) {}

  getEvents(searchTerm?: string): Observable<Event[]> {
    let params = new HttpParams();
    if (searchTerm) {
      params = params.set('search', searchTerm);
    }
    return this.http.get<Event[]>(this.apiUrl, { params });
  }

  getEventById(eventId: string): Observable<Event> {
    const url = `${this.apiUrl}/${eventId}`;
    return this.http.get<Event>(url);
  }

  getEventDetails(eventId: string): Observable<EventDetails> {
    const url = `${this.apiUrl}/${eventId}/details`;
    return this.http.get<EventDetails>(url);
  }

  getEventPoster(eventId: string): Observable<Blob> {
    const url = `${this.apiUrl}/${eventId}/poster`;
    return this.http.get(url, { responseType: 'blob' });
  }
}
