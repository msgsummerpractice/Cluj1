import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { CheckInRequest } from '../../../core/models/check-in-request.model';

@Injectable({
  providedIn: 'root',
})
export class EventCheckInService {
  private apiUrl = '/api/events/checkin';
  private http = inject(HttpClient);

  checkIn(request: CheckInRequest): Observable<void> {
    return this.http.post<void>(this.apiUrl, request);
  }
}
