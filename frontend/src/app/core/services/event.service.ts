import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class EventService {
  private apiUrl: string = 'http://localhost:8080/api/events';
  private http = inject(HttpClient);

  getUpcomingRegisteredEventsCountPerUser(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/countRegistrationPerUser`);
  }
}
