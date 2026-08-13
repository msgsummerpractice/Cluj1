import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { TranslocoService, TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../../../core/services/auth.service';
import { LanguageService } from '../../../core/services/language-service';

@Component({
  selector: 'app-role-home',
  imports: [CommonModule, MatButtonModule, TranslocoModule],
  templateUrl: './role-home.html',
  styleUrl: './role-home.css',
})
export class RoleHomeComponent {
  protected readonly authService = inject(AuthService);
  private readonly languageService = inject(LanguageService);
  private readonly translocoService = inject(TranslocoService);

  protected readonly roleLabel = computed(() => {
    this.languageService.currentLanguage();
    const role = this.authService.currentRole() || 'PARTICIPANT';
    return this.translocoService.translate(`roles.labels.${role}`);
  });

  protected readonly roleSummary = computed(() => {
    this.languageService.currentLanguage();
    const role = this.authService.currentRole() || 'PARTICIPANT';
    return this.translocoService.translate(`roles.summaries.${role}`);
  });
}
