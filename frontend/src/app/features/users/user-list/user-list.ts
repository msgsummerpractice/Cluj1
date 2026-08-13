// src/app/features/users/user-list/user-list.ts

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormControl } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { User } from '../../../core/models/user.model';
import { UserService } from '../../../core/services/user.service';

import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { RoleManageDialogComponent } from '../role-manage-dialog/role-manage-dialog';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TranslocoModule,
    MatTableModule,
    MatInputModule,
    MatFormFieldModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatSlideToggleModule,
    MatDialogModule,
    MatSnackBarModule,
  ],
  templateUrl: './user-list.html',
  styleUrl: './user-list.css',
})
export class UserListComponent implements OnInit {
  users: User[] = [];
  displayedColumns: string[] = [
    'firstName',
    'lastName',
    'email',
    'role',
    'location',
    'isActive',
    'actions',
  ];

  searchControl = new FormControl('');

  constructor(
    private userService: UserService,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private translocoService: TranslocoService,
  ) {}

  ngOnInit(): void {
    this.fetchUsers();

    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((term) => {
        this.fetchUsers(term || '');
      });
  }

  fetchUsers(search?: string): void {
    this.userService.getUsers(search).subscribe({
      next: (data) => {
        this.users = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error fetching users', err),
    });
  }

  manageRole(user: User): void {
    const dialogRef = this.dialog.open(RoleManageDialogComponent, {
      width: '500px',
      data: { user },
      panelClass: 'custom-dialog-container',
    });

    dialogRef.afterClosed().subscribe((newRole) => {
      if (newRole && newRole !== user.role) {
        const confirmMessage = this.translocoService.translate('notifications.confirmRoleChange', {
          email: user.email,
          role: newRole,
        });

        if (confirm(confirmMessage)) {
          this.userService.updateRole(user.id, newRole).subscribe({
            next: (updatedUser) => {
              const index = this.users.findIndex((u) => u.id === updatedUser.id);
              if (index !== -1) this.users[index] = updatedUser;
              this.users = [...this.users];
              this.showNotification(this.translocoService.translate('notifications.roleUpdated'));
            },
            error: (err) =>
              this.showNotification(
                err.error?.message ||
                  this.translocoService.translate('notifications.errorUpdatingRole'),
                'error',
              ),
          });
        }
      }
    });
  }

  toggleStatus(user: User): void {
    const newStatus = !user.isActive;
    this.userService.updateStatus(user.id, newStatus).subscribe({
      next: (updatedUser) => {
        const index = this.users.findIndex((u) => u.id === updatedUser.id);
        if (index !== -1) this.users[index] = updatedUser;
        this.users = [...this.users];

        const messageKey = newStatus
          ? 'notifications.userActivated'
          : 'notifications.userDeactivated';
        this.showNotification(this.translocoService.translate(messageKey));
      },
      error: (err) => {
        user.isActive = !newStatus;
        this.showNotification(
          err.error?.message ||
            this.translocoService.translate('notifications.errorUpdatingStatus'),
          'error',
        );
      },
    });
  }

  private showNotification(message: string, type: 'success' | 'error' = 'success'): void {
    this.snackBar.open(message, 'Close', {
      duration: 4000,
      panelClass: type === 'error' ? ['bg-red-500', 'text-white'] : ['bg-green-600', 'text-white'],
    });
  }
}
