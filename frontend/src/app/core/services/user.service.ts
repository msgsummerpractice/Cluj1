import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User } from '../models/user.model';
import { RegisterDto } from '../models/register-dto';
import { UserProfile } from '../models/user-profile.model';
import { Page } from '../models/page.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private baseUrl: string = environment.apiUrl;
  private http = inject(HttpClient);

  getUsers(searchTerm?: string, page: number = 0, size: number = 10): Observable<Page<User>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    if (searchTerm) {
      params = params.set('search', searchTerm);
    }
    return this.http.get<Page<User>>(`${this.baseUrl}/api/users`, { params });
  }

  registerUser(userData: RegisterDto): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/users/register`, userData);
  }
  updateRole(userId: string, role: string): Observable<User> {
    return this.http.patch<User>(`${this.baseUrl}/api/users/${userId}/role`, { role });
  }

  updateStatus(userId: string, isActive: boolean): Observable<User> {
    const params = new HttpParams().set('isActive', isActive.toString());
    return this.http.patch<User>(`${this.baseUrl}/api/users/${userId}/status`, {}, { params });
  }
  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.baseUrl}/api/users/profile`);
  }

  updateProfile(userLocation?: string, profilePicture?: File): Observable<void> {
    const formData = new FormData();
    if (userLocation) {
      formData.append('userLocation', userLocation);
    }
    if (profilePicture) {
      formData.append('profilePicture', profilePicture);
    }
    return this.http.patch<void>(`${this.baseUrl}/api/users/profile`, formData);
  }
}
