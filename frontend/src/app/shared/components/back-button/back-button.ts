import { Location } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';

@Component({
  selector: 'app-back-button',
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './back-button.html',
  styleUrl: './back-button.css',
})
export class BackButtonComponent {
  private readonly location = inject(Location);
  private readonly router = inject(Router);

  readonly backRoute = input<string | readonly unknown[]>();

  goBack(): void {
    const backRoute = this.backRoute();

    if (backRoute) {
      if (typeof backRoute === 'string') {
        void this.router.navigateByUrl(backRoute);
      } else {
        void this.router.navigate([...backRoute]);
      }
      return;
    }

    this.location.back();
  }
}
