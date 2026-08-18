import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/auth/auth.guards';
import { RegisterComponent } from './features/register-component/register-component';
import { ProfileComponent } from './features/profile-component/profile-component';

export const routes: Routes = [
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
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
    path: 'events/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/events/event-details/event-details').then((m) => m.EventDetailsComponent),
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
    path: '**',
    redirectTo: 'login',
  },
];
