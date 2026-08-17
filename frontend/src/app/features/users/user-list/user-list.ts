import { Component, OnInit, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormControl } from '@angular/forms';
import { debounceTime, distinctUntilChanged, map } from 'rxjs';
import { User } from '../../../core/models/user.model';
import { UserService } from '../../../core/services/user.service';
import { Page } from '../../../core/models/page.model';

import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule, MatSlideToggleChange } from '@angular/material/slide-toggle';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { RoleManageDialogComponent } from '../role-manage-dialog/role-manage-dialog';

import { DataTableComponent } from '../../../shared/components/data-table/data-table';
import { DataTableColumn } from '../../../shared/components/data-table/data-table.model';
import { DataTableCellDefDirective } from '../../../shared/components/data-table/data-table-cell-def.directive';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TranslocoModule,
    MatInputModule,
    MatFormFieldModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatDialogModule,
    MatSnackBarModule,
    MatPaginatorModule,
    DataTableComponent,
    DataTableCellDefDirective,
  ],
  templateUrl: './user-list.html',
  styleUrl: './user-list.css',
})
export class UserListComponent implements OnInit {
  users = signal<User[]>([]);
  totalUsers = signal<number>(0);
  pageSize = signal<number>(10);
  currentPage = signal<number>(0);

  tableColumns: DataTableColumn[] = [
    { key: 'firstName', label: 'userList.colFirstName' },
    { key: 'lastName', label: 'userList.colLastName' },
    { key: 'email', label: 'userList.colEmail' },
    { key: 'role', label: 'userList.colRole' },
    { key: 'location', label: 'userList.colLocation' },
    { key: 'isActive', label: 'userList.colStatus' },
    { key: 'actions', label: 'userList.colActions' },
  ];

  searchControl = new FormControl('');

  private userService = inject(UserService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private translocoService = inject(TranslocoService);
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.fetchUsers();

    this.searchControl.valueChanges
      .pipe(
        debounceTime(300),
        map((term) => (term || '').trim()),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((term) => {
        this.currentPage.set(0);
        this.fetchUsers(term || '');
      });
  }

  fetchUsers(search?: string): void {
    const searchTerm = search ?? this.searchControl.value ?? '';
    this.userService
      .getUsers(searchTerm, this.currentPage(), this.pageSize())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data: Page<User>) => {
          this.users.set(data.content);
          this.totalUsers.set(data.totalElements);
        },
        error: (err) => console.error('Error fetching users', err),
      });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.fetchUsers();
  }

  manageRole(user: User): void {
    const dialogRef = this.dialog.open(RoleManageDialogComponent, {
      width: '500px',
      data: { user },
      panelClass: 'custom-dialog-container',
    });

    dialogRef
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((updatedUser: User | undefined) => {
        if (updatedUser) {
          this.users.update((currentUsers) => {
            const index = currentUsers.findIndex((u) => u.id === updatedUser.id);
            if (index !== -1) {
              const newUsers = [...currentUsers];
              newUsers[index] = updatedUser;
              return newUsers;
            }
            return currentUsers;
          });
        }
      });
  }

  toggleStatus(user: User, event: MatSlideToggleChange): void {
    const newStatus = event.checked;
    this.userService
      .updateStatus(user.id, newStatus)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedUser) => {
          this.users.update((currentUsers) => {
            const index = currentUsers.findIndex((u) => u.id === updatedUser.id);
            if (index !== -1) {
              const newUsers = [...currentUsers];
              newUsers[index] = updatedUser;
              return newUsers;
            }
            return currentUsers;
          });

          const messageKey = newStatus
            ? 'notifications.userActivated'
            : 'notifications.userDeactivated';
          this.showNotification(this.translocoService.translate(messageKey));
        },
        error: (err) => {
          event.source.checked = user.isActive;

          this.showNotification(
            err.error?.message ||
              this.translocoService.translate('notifications.errorUpdatingStatus'),
            'error',
          );
        },
      });
  }

  formatLocation(location: string): string {
    if (!location) return '';
    const formatted = location.replace(/_/g, ' ').toLowerCase();
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  }

  private showNotification(message: string, type: 'success' | 'error' = 'success'): void {
    this.snackBar.open(message, 'Close', {
      duration: 4000,
      panelClass: type === 'error' ? ['bg-red-500', 'text-white'] : ['bg-green-600', 'text-white'],
    });
  }
}
