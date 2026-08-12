import { Routes } from '@angular/router';
import { UserListComponent } from './features/users/user-list/user-list';
import { EventListComponent } from './features/events/event-list/event-list';

export const routes: Routes = [
  { path: 'admin/users', component: UserListComponent },
  { path: 'events', component: EventListComponent },
];
