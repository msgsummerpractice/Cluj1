import { Component, EventEmitter, Output } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-clear-filter',
  imports: [MatIconButton, MatIcon, MatTooltip, TranslocoPipe],
  templateUrl: './clear-filter.html',
  styleUrls: ['./clear-filter.css'],
})
export class ClearFilter {
  @Output() clear = new EventEmitter<void>();

  onClear() {
    this.clear.emit();
  }
}
