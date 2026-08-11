import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User } from '../models/user.model';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private apiUrl = '/api/users';

  constructor(private http: HttpClient) {}

  getUsers(searchTerm?: string): Observable<User[]> {
    let params = new HttpParams();
    if (searchTerm) {
      params = params.set('search', searchTerm);
    }
    return this.http.get<User[]>(this.apiUrl, { params });
  }
}
