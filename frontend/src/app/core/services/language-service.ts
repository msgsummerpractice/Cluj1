import { Injectable, inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { toSignal } from '@angular/core/rxjs-interop';
import { Language } from '../models/language-model';

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translocoService = inject(TranslocoService);

  readonly supportedLanguages: readonly Language[] = [
    { code: 'en', label: 'English' },
    { code: 'ro', label: 'Română' },
  ];

  readonly currentLanguage = toSignal(this.translocoService.langChanges$, {
    initialValue: this.translocoService.getActiveLang(),
  });

  setLanguage(code: string): void {
    this.translocoService.setActiveLang(code);
  }
}
