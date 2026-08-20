import { inject } from '@angular/core';
import { Routes, Router } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/auth/auth.guards';
import { AuthService } from './core/services/auth.service';
import { RegisterComponent } from './features/register-component/register-component';
import { ProfileComponent } from './features/profile-component/profile-component';
import { EventCheckInComponent } from './features/events/event-checkin/event-checkin';
import { RubiksCubeComponent } from './features/rubiks-cube-component/rubiks-cube-component';
import { NotFoundComponent } from './shared/components/not-found/not-found';

export const routes: Routes = [
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  { path: 'rubik', component: RubiksCubeComponent },
  {
    path: 'register',
    component: RegisterComponent,
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login').then((m) => m.LoginComponent),
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password').then(
        (m) => m.ForgotPasswordComponent,
      ),
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password').then((m) => m.ResetPasswordComponent),
  },
  {
    path: 'participant',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['PARTICIPANT'] },
    loadComponent: () =>
      import('./features/auth/role-home/role-home').then((m) => m.RoleHomeComponent),
  },
  {
    path: 'marketing',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['MARKETING_ORGANIZER'] },
    loadComponent: () =>
      import('./features/auth/role-home/role-home').then((m) => m.RoleHomeComponent),
  },
  {
    path: 'hr',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['HR_USER'] },
    loadComponent: () =>
      import('./features/auth/role-home/role-home').then((m) => m.RoleHomeComponent),
  },
  {
    path: 'admin/users',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/users/user-list/user-list').then((m) => m.UserListComponent),
  },
  {
    path: 'events',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['PARTICIPANT', 'MARKETING_ORGANIZER', 'HR_USER', 'ADMIN'] },
    loadComponent: () =>
      import('./features/events/event-list/event-list').then((m) => m.EventListComponent),
  },
  {
    path: 'events/create',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['MARKETING_ORGANIZER'] },
    loadComponent: () =>
      import('./features/events/event-creation/event-creation').then(
        (m) => m.EventCreationComponent,
      ),
  },
  {
    path: 'events/:id/register',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['MARKETING_ORGANIZER', 'HR_USER', 'ADMIN', 'USER', 'PARTICIPANT'] },
    loadComponent: () =>
      import('./features/events/event-registration/event-registration').then(
        (m) => m.EventRegistration,
      ),
  },
  {
    path: 'events/:id/edit',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['MARKETING_ORGANIZER'] },
    loadComponent: () =>
      import('./features/events/event-creation/event-creation').then(
        (m) => m.EventCreationComponent,
      ),
  },
  {
    path: 'events/:id/checkin',
    component: EventCheckInComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['MARKETING_ORGANIZER', 'HR_USER', 'ADMIN', 'PARTICIPANT'] },
  },
  {
    path: 'events/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/events/event-details/event-details').then((m) => m.EventDetailsComponent),
  },
  {
    path: 'not-found',
    component: NotFoundComponent,
  },
  {
    path: 'homepage',
    canActivate: [
      () => {
        const authService = inject(AuthService);
        const router = inject(Router);
        return router.createUrlTree([authService.getHomeRoute()]);
      },
    ],
    children: [],
  },
  {
    path: '**',
    redirectTo: 'not-found',
  },
];
