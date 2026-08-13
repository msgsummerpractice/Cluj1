import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User } from '../models/user.model';
import { RegisterDto } from '../models/register-dto';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private apiUrl: string = 'http://localhost:8080/api/users';
  private http = inject(HttpClient);

  getUsers(searchTerm?: string): Observable<User[]> {
    let params = new HttpParams();
    if (searchTerm) {
      params = params.set('search', searchTerm);
    }
    return this.http.get<User[]>(this.apiUrl, { params });
  }

  registerUser(userData: RegisterDto): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData);
  }

  updateRole(userId: string, role: string): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/${userId}/role`, { role });
  }

  updateStatus(userId: string, isActive: boolean): Observable<User> {
    const params = new HttpParams().set('isActive', isActive.toString());
    return this.http.patch<User>(`${this.apiUrl}/${userId}/status`, {}, { params });
  }
}
