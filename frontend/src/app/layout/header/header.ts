import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { LanguageSwitcher } from '../language-switch/language-switcher';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { TranslocoModule } from '@jsverse/transloco';

@Component({
  selector: 'app-header',
  imports: [
    CommonModule,
    RouterLink,
    LanguageSwitcher,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatToolbarModule,
    TranslocoModule,
  ],
  templateUrl: './header.html',
})
export class Header {
  protected readonly authService = inject(AuthService);

  showHeader() {
    return this.authService.isAuthenticated();
  }
}
