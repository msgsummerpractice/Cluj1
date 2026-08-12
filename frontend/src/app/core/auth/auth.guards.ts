import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/auth-user.model';

export const authGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isAuthenticated()
    ? true
    : router.createUrlTree(['/login'], {
        queryParams: { returnUrl: state.url },
      });
};

export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isAuthenticated() ? router.createUrlTree([authService.getHomeRoute()]) : true;
};

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const allowedRoles = (route.data?.['roles'] as UserRole[] | undefined) ?? [];
  const currentRole = authService.currentRole();

  if (!currentRole) {
    return router.createUrlTree(['/login']);
  }

  return allowedRoles.length === 0 || allowedRoles.includes(currentRole)
    ? true
    : router.createUrlTree([authService.getHomeRoute()]);
};
