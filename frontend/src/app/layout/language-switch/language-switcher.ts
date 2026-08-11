import { Component, inject } from '@angular/core';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { LanguageService } from '../../core/services/language-service';

@Component({
  selector: 'app-language-switcher',
  standalone: true,
  imports: [MatMenuModule, MatButtonModule, MatIconModule],
  templateUrl: './language-switcher.html',
  styleUrl: './language-switcher.css',
})
export class LanguageSwitcher {
  protected readonly languageService = inject(LanguageService);
}
