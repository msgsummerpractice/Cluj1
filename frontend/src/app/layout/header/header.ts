import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';

@Component({
  selector: 'app-header',
  imports: [MatButtonModule, MatToolbarModule, MatIconModule, MatMenuModule],
  templateUrl: './header.html',
})
export class Header {}
