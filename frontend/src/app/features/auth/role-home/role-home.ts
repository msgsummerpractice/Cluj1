import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-role-home',
  imports: [CommonModule, MatButtonModule],
  templateUrl: './role-home.html',
  styleUrl: './role-home.css',
})
export class RoleHomeComponent {
  protected readonly authService = inject(AuthService);

  protected readonly roleLabel = computed(() => {
    switch (this.authService.currentRole()) {
      case 'MARKETING_ORGANIZER':
        return 'Marketing Organizer';
      case 'HR_USER':
        return 'HR User';
      case 'ADMIN':
        return 'Admin';
      case 'PARTICIPANT':
      default:
        return 'Participant';
    }
  });

  protected readonly roleSummary = computed(() => {
    switch (this.authService.currentRole()) {
      case 'MARKETING_ORGANIZER':
        return 'Plan communication, coordinate promotion, and keep event campaigns moving.';
      case 'HR_USER':
        return 'Monitor staffing-related event information and manage people-focused workflows.';
      case 'ADMIN':
        return 'Manage the application and control access across the event platform.';
      case 'PARTICIPANT':
      default:
        return 'View your event space and continue the actions available for your account.';
    }
  });
}
