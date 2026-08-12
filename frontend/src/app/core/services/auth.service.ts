import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, map } from 'rxjs';
import { AuthRequest } from '../models/auth-request.model';
import { AuthResponse } from '../models/auth-response.model';
import { AuthUser, UserRole } from '../models/auth-user.model';

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
  private readonly authUserState = signal<AuthUser | null>(this.restoreSession());

  readonly currentUser = computed(() => this.authUserState());
  readonly currentRole = computed(() => this.authUserState()?.role ?? null);
  readonly isAuthenticated = computed(() => this.authUserState() !== null);

  login(request: AuthRequest): Observable<AuthUser> {
    return this.http
      .post<AuthResponse>('/api/auth/login', request)
      .pipe(map(({ token }) => this.persistSession(token)));
  }

  logout(): void {
    this.clearSession();
    this.http.post<void>('/api/auth/logout', {}).subscribe({
      error: () => undefined,
    });
    void this.router.navigate(['/login']);
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

    if (!this.authUserState()) {
      this.clearSession();
      return null;
    }

    return token;
  }

  getLandingRoute(role: UserRole): string {
    switch (role) {
      case 'ADMIN':
        return '/admin/users';
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

    return user;
  }

  private parseToken(token: string): AuthUser | null {
    try {
      const [, encodedPayload] = token.split('.');
      if (!encodedPayload) {
        return null;
      }

      const normalizedPayload = encodedPayload.replace(/-/g, '+').replace(/_/g, '/');
      const paddedPayload = normalizedPayload.padEnd(
        normalizedPayload.length + ((4 - (normalizedPayload.length % 4)) % 4),
        '=',
      );
      const payload = JSON.parse(atob(paddedPayload)) as JwtPayload;

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

  private getStoredToken(): string | null {
    return localStorage.getItem(this.storageKey);
  }

  private clearSession(): void {
    localStorage.removeItem(this.storageKey);
    this.authUserState.set(null);
  }
}
