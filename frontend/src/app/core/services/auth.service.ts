import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, finalize, map } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { AuthRequest } from '../models/auth-request.model';
import { AuthResponse } from '../models/auth-response.model';
import { AuthUser, UserRole } from '../models/auth-user.model';
import { environment } from '../../../environments/environment';

type JwtPayload = {
  sub?: string;
  email?: string;
  role?: UserRole;
  exp?: number;
  iat?: number;
};

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly storageKey = 'eventapp.auth.token';
  private expirationTimer: ReturnType<typeof setTimeout> | null = null;
  // private apiUrl: string = 'http://localhost:8080/api/auth';
  private baseUrl: string = environment.apiUrl;

  private readonly authUserState = signal<AuthUser | null>(this.restoreSession());

  readonly currentUser = computed(() => {
    const user = this.authUserState();
    if (!user || user.exp * 1000 <= Date.now()) {
      return null;
    }
    return user;
  });

  readonly currentRole = computed(() => this.currentUser()?.role ?? null);
  readonly isAuthenticated = computed(() => this.currentUser() !== null);

  login(request: AuthRequest): Observable<AuthUser> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/api/auth/login`, request)
      .pipe(map(({ token }) => this.persistSession(token)));
  }

  logout(): void {
    this.http
      .post<void>(`${this.baseUrl}/api/auth/logout`, {})
      .pipe(
        finalize(() => {
          this.clearSession();
          void this.router.navigate(['/login']);
        }),
      )
      .subscribe();
  }

  expireSession(): void {
    this.clearSession();
    void this.router.navigate(['/login']);
  }

  getToken(): string | null {
    const token = this.getStoredToken();
    if (!token) {
      return null;
    }

    if (!this.currentUser()) {
      this.clearSession();
      return null;
    }

    return token;
  }

  getLandingRoute(role: UserRole): string {
    switch (role) {
      case 'ADMIN':
        return '/admin';
      case 'MARKETING_ORGANIZER':
        return '/marketing';
      case 'HR_USER':
        return '/hr';
      case 'PARTICIPANT':
      default:
        return '/participant';
    }
  }

  getHomeRoute(): string {
    const role = this.currentRole();
    return role ? this.getLandingRoute(role) : '/login';
  }

  private persistSession(token: string): AuthUser {
    const user = this.parseToken(token);
    if (!user) {
      throw new Error('Received an invalid authentication token.');
    }

    localStorage.setItem(this.storageKey, token);
    this.authUserState.set(user);
    this.scheduleExpiration(user.exp);
    return user;
  }

  private restoreSession(): AuthUser | null {
    const token = this.getStoredToken();
    if (!token) {
      return null;
    }

    const user = this.parseToken(token);
    if (!user) {
      this.clearSession();
      return null;
    }

    this.scheduleExpiration(user.exp);
    return user;
  }

  private parseToken(token: string): AuthUser | null {
    try {
      const payload = jwtDecode<JwtPayload>(token);

      if (!payload.sub || !payload.email || !payload.role || !payload.exp || !payload.iat) {
        return null;
      }

      if (payload.exp * 1000 <= Date.now()) {
        return null;
      }

      return {
        id: payload.sub,
        email: payload.email,
        role: payload.role,
        exp: payload.exp,
        iat: payload.iat,
      };
    } catch {
      return null;
    }
  }

  private scheduleExpiration(expInSeconds: number): void {
    if (this.expirationTimer) {
      clearTimeout(this.expirationTimer);
    }

    const msUntilExpiration = expInSeconds * 1000 - Date.now();
    if (msUntilExpiration <= 0) {
      this.expireSession();
      return;
    }

    this.expirationTimer = setTimeout(() => {
      this.expireSession();
    }, msUntilExpiration);
  }

  private getStoredToken(): string | null {
    return localStorage.getItem(this.storageKey);
  }

  private clearSession(): void {
    if (this.expirationTimer) {
      clearTimeout(this.expirationTimer);
      this.expirationTimer = null;
    }
    localStorage.removeItem(this.storageKey);
    this.authUserState.set(null);
  }

  forgotPassword(email: string): Observable<string> {
    return this.http.post(
      `${this.baseUrl}/api/auth/forgot-password`,
      { email },
      { responseType: 'text' },
    );
  }

  resetPassword(payload: {
    token: string;
    newPassword: string;
    confirmPassword: string;
  }): Observable<string> {
    return this.http.post(`${this.baseUrl}/api/auth/reset-password`, payload, {
      responseType: 'text',
    });
  }

  isAdmin(): boolean {
    return this.currentRole() === 'ADMIN';
  }

  isMarketingOrganizer(): boolean {
    return this.currentRole() === 'MARKETING_ORGANIZER';
  }

  isHrUser(): boolean {
    return this.currentRole() === 'HR_USER';
  }

  isParticipant(): boolean {
    return this.currentRole() === 'PARTICIPANT';
  }
}
