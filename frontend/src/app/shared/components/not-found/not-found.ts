import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterLink, TranslocoModule],
  template: `
    <div
      class="flex flex-col items-center justify-center min-h-screen gap-6 p-8 text-center bg-gray-50"
    >
      <span class="material-symbols-outlined text-6xl text-gray-300">search_off</span>
      <div>
        <h1 class="text-2xl font-bold text-gray-800 mb-2">
          {{ 'notFound.title' | transloco }}
        </h1>
        <p class="text-gray-500 text-sm">{{ 'notFound.subtitle' | transloco }}</p>
      </div>
      <a
        routerLink="/events"
        class="px-6 py-2 rounded-full text-sm font-medium text-white"
        style="background-color: #8a1538"
      >
        {{ 'notFound.back' | transloco }}
      </a>
    </div>
  `,
  styles: [
    `
      .material-symbols-outlined {
        font-family: 'Material Symbols Outlined';
        font-size: 64px;
        line-height: 1;
      }
    `,
  ],
})
export class NotFoundComponent {}
