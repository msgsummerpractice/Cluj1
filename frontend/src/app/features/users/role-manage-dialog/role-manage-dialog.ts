// src/app/features/users/role-manage-dialog/role-manage-dialog/role-manage-dialog.ts

import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { User } from '../../../core/models/user.model';
import { MatIconModule } from '@angular/material/icon';

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
  ],
  templateUrl: './role-manage-dialog.html',
  styleUrl: './role-manage-dialog.css',
})
export class RoleManageDialogComponent {
  selectedRole: string;
  roles = ['PARTICIPANT', 'MARKETING_ORGANIZER', 'HR_USER', 'ADMIN'];

  constructor(
    public dialogRef: MatDialogRef<RoleManageDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { user: User },
  ) {
    this.selectedRole = data.user.role;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    this.dialogRef.close(this.selectedRole);
  }
}
