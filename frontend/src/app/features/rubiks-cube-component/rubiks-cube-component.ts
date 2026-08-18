import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-rubiks-cube-component',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rubiks-cube-component.html',
  styleUrl: './rubiks-cube-component.css',
})
export class RubiksCubeComponent {
  cubies: any[] = [];

  constructor() {
    let id = 0;
    for (let y = -1; y <= 1; y++) {
      for (let x = -1; x <= 1; x++) {
        for (let z = -1; z <= 1; z++) {
          let letter = '';
          let topLetter = '';

          if (z === 1 && y === 1) {
            if (x === -1) letter = '.m';
            if (x === 0) letter = 's';
            if (x === 1) letter = 'g';

          }
          if(z === 1 && y === 0) {
            if(x === 0) letter = 't';
          }
          if (z === 1 && y === -1) {
            if (x === 0) letter = 'n';
          }

          if (y === -1 && x === 0 && z === 1) {
            topLetter = 'e';
          }

          if(y === -1 && x === 0 && z === 0) {
            topLetter = 'v';
          }
          if (y === -1 && x === 0 && z === -1) {
            topLetter = 'e';
          }



          // Assign row classes (Horizontal moves)
          let rowClass = '';
          if (y === -1) rowClass = 'row-top';
          if (y === 0) rowClass = 'row-middle';
          if (y === 1) rowClass = 'row-bottom';

          // Assign column classes (Vertical moves)
          let colClass = '';
          if (x === -1) colClass = 'col-left';
          if (x === 0) colClass = 'col-center';
          if (x === 1) colClass = 'col-right';

          this.cubies.push({ id: id++, x, y, z, letter,topLetter, rowClass, colClass });
        }
      }
    }
  }
}
