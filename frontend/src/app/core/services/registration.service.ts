import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EventRegistrationRequest } from '../models/event-registration.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class RegistrationService {
  // private apiUrl: string = 'http://localhost:8080/api/registration';
  private baseUrl = environment.apiUrl;
  private http = inject(HttpClient);

  getRegistrationCountPerUser(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/api/registration/count`);
  }
}
