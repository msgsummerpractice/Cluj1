import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { TranslocoService, TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-role-home',
  imports: [CommonModule, MatButtonModule, TranslocoModule],
  templateUrl: './role-home.html',
  styleUrl: './role-home.css',
})
export class RoleHomeComponent {
  protected readonly authService = inject(AuthService);
  private readonly translocoService = inject(TranslocoService);

  protected readonly roleLabel = computed(() => {
    const role = this.authService.currentRole() || 'PARTICIPANT';
    return this.translocoService.translate(`roles.labels.${role}`);
  });

  protected readonly roleSummary = computed(() => {
    const role = this.authService.currentRole() || 'PARTICIPANT';
    return this.translocoService.translate(`roles.summaries.${role}`);
  });
}
