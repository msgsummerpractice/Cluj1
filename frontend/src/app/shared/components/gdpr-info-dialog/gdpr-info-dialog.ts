import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { TranslocoModule } from '@jsverse/transloco';
import { GdprInfoDialogData } from './gdpr-info-dialog.model';

@Component({
  selector: 'app-gdpr-info-dialog',
  imports: [MatButtonModule, MatDialogModule, TranslocoModule],
  templateUrl: './gdpr-info-dialog.html',
  styleUrl: './gdpr-info-dialog.css',
})
export class GdprInfoDialogComponent {
  readonly data = inject<GdprInfoDialogData>(MAT_DIALOG_DATA);

  private readonly dialogRef = inject(MatDialogRef<GdprInfoDialogComponent>);

  close(): void {
    this.dialogRef.close();
  }
}
