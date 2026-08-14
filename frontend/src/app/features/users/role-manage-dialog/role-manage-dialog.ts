import { Component, inject, Inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { User } from '../../../core/models/user.model';
import { Role } from '../../../core/models/role.enum';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-role-manage-dialog-component',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatSelectModule,
    MatFormFieldModule,
    MatIconModule,
    MatSnackBarModule,
    TranslocoModule,
  ],
  templateUrl: './role-manage-dialog.html',
  styleUrl: './role-manage-dialog.css',
})
export class RoleManageDialogComponent {
  selectedRole: Role;
  originalRole: Role;
  roles: Role[] = Object.values(Role);
  isConfirming = false;
  isLoading = false;

  private snackBar = inject(MatSnackBar);
  private userService = inject(UserService);
  private translocoService = inject(TranslocoService);
  private destroyRef = inject(DestroyRef);

  constructor(
    public dialogRef: MatDialogRef<RoleManageDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { user: User },
  ) {
    this.selectedRole = data.user.role as unknown as Role;
    this.originalRole = data.user.role as unknown as Role;
  }

  onCancel(): void {
    if (this.isConfirming) {
      this.isConfirming = false;
    } else {
      this.dialogRef.close();
    }
  }

  onSave(): void {
    if (this.selectedRole === this.originalRole) {
      this.dialogRef.close();
      return;
    }

    if (!this.isConfirming) {
      this.isConfirming = true;
      return;
    }

    this.isLoading = true;
    this.userService
      .updateRole(this.data.user.id, this.selectedRole)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedUser) => {
          this.snackBar.open(
            this.translocoService.translate('roleManageDialog.successMessage'),
            'Close',
            { duration: 3000, panelClass: ['success-snackbar'] },
          );
          this.dialogRef.close(updatedUser);
        },
        error: (err) => {
          this.isLoading = false;
          const errMsg =
            err.error?.message || this.translocoService.translate('roleManageDialog.errorMessage');
          this.snackBar.open(errMsg, 'Close', { duration: 4000, panelClass: ['error-snackbar'] });
        },
      });
  }
}
