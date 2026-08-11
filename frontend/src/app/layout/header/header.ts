import { Component } from '@angular/core';
import { LanguageSwitcher } from '../language-switch/language-switcher';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { TranslocoModule } from '@jsverse/transloco';

@Component({
  selector: 'app-header',
  imports: [
    MatButtonModule,
    MatToolbarModule,
    MatIconModule,
    MatMenuModule,
    TranslocoModule,
    LanguageSwitcher,
  ],
  templateUrl: './header.html',
})
export class Header {}
