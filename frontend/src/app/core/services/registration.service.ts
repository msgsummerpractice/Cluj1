import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})

export class RegistrationService{
  private apiUrl:string = 'http://localhost:8080/api/registration';
  private http = inject(HttpClient);

  getRegistrationCountPerUser(): Observable<number>{
    return this.http.get<number>(`${this.apiUrl}/count`);
  }


}
