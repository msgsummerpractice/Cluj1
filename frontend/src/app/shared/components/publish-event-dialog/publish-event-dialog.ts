import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { TranslocoModule } from '@jsverse/transloco';

@Component({
  selector: 'app-publish-event-dialog',
  imports: [MatButtonModule, MatDialogModule, TranslocoModule],
  templateUrl: './publish-event-dialog.html',
  styleUrl: './publish-event-dialog.css',
})
export class PublishEventDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<PublishEventDialogComponent>);

  cancel(): void {
    this.dialogRef.close(false);
  }

  confirm(): void {
    this.dialogRef.close(true);
  }
}
