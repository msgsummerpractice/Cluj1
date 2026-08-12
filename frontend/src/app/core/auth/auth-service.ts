import { inject, Injectable } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {RegisterDto} from '../models/register-dto';
import {Observable} from 'rxjs';

@Injectable({providedIn: 'root'})
export class AuthService {

  private apiUrl: string = 'http://localhost:8080/api/users';
  private http = inject(HttpClient);

  registerUser(userData: RegisterDto): Observable<any>{
    return this.http.post(`${this.apiUrl}/register`, userData);

  }
}
