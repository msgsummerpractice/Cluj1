import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User } from '../models/user.model';
import { RegisterDto } from '../models/register-dto';
import { UserProfile } from '../models/user-profile.model';

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

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/profile`);
  }

  updateProfile(userLocation?: string, profilePicture?: File): Observable<void> {
    const formData = new FormData();
    if (userLocation) {
      formData.append('userLocation', userLocation);
    }
    if (profilePicture) {
      formData.append('profilePicture', profilePicture);
    }
    return this.http.patch<void>(`${this.apiUrl}/profile`, formData);
  }
}
