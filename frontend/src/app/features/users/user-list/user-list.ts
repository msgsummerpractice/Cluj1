import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { User } from '../../../core/models/user.model';
import { UserService } from '../../../core/services/user.service';

import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { ChangeDetectorRef } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { DataTableComponent } from '../../../shared/components/data-table/data-table';
import { DataTableCellDefDirective } from '../../../shared/components/data-table/data-table-cell-def.directive';
import { DataTableColumn } from '../../../shared/components/data-table/data-table.model';

@Component({
  selector: 'app-user-list',
  imports: [
    CommonModule,
    FormsModule,
    TranslocoModule,
    MatInputModule,
    MatFormFieldModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    DataTableComponent,
    DataTableCellDefDirective,
  ],
  templateUrl: './user-list.html',
  styleUrl: './user-list.css',
})
export class UserListComponent implements OnInit {
  users: User[] = [];
  readonly columns: readonly DataTableColumn[] = [
    { key: 'firstName', label: 'userList.colFirstName' },
    { key: 'lastName', label: 'userList.colLastName' },
    { key: 'email', label: 'userList.colEmail', cellClass: 'text-gray-600' },
    { key: 'role', label: 'userList.colRole' },
    { key: 'location', label: 'userList.colLocation' },
    { key: 'isActive', label: 'userList.colStatus' },
    {
      key: 'actions',
      label: 'userList.colActions',
      headerClass: 'text-center',
      cellClass: 'text-center',
    },
  ];
  searchTerm: string = '';
  searchSubject: Subject<string> = new Subject<string>();

  constructor(
    private userService: UserService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.fetchUsers();

    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe((term) => {
      this.fetchUsers(term);
    });
  }

  onSearch(term: string): void {
    this.searchSubject.next(term);
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

  manageRole(userId: string): void {
    this.router.navigate(['/admin/users', userId, 'manage-role']);
  }
}
