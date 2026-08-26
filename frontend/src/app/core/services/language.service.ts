import { Injectable, inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { toSignal } from '@angular/core/rxjs-interop';
import { Language } from '../models/language.model';

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translocoService = inject(TranslocoService);

  private readonly STORAGE_KEY = 'app_language';
  private readonly FALLBACK_LANG = 'en';

  readonly supportedLanguages: readonly Language[] = [
    { code: 'en', label: 'English' },
    { code: 'ro', label: 'Română' },
  ];

  readonly currentLanguage = toSignal(this.translocoService.langChanges$, {
    initialValue: this.translocoService.getActiveLang(),
  });

  constructor() {
    this.initLanguage();
  }

  setLanguage(code: string): void {
    const langCode = this.isSupported(code) ? code : this.FALLBACK_LANG;
    this.translocoService.setActiveLang(langCode);
    localStorage.setItem(this.STORAGE_KEY, langCode);
  }

  private initLanguage(): void {
    const storedLang = localStorage.getItem(this.STORAGE_KEY);

    if(storedLang && this.isSupported(storedLang)) {
      this.setLanguage(storedLang);
      return;
    }

    const browserLang = navigator.language?.split('-')[0].toLowerCase();
    if(browserLang && this.isSupported(browserLang)) {
      this.setLanguage(browserLang);
      return;
    }

    this.setLanguage(this.FALLBACK_LANG);
  }

  private isSupported(code: string): boolean {
    return this.supportedLanguages.some((lang) => lang.code === code);
  }
}
