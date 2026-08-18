import { Location } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-back-button',
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './back-button.html',
  styleUrl: './back-button.css',
})
export class BackButtonComponent {
  private readonly location = inject(Location);

  goBack(): void {
    this.location.back();
  }
}
