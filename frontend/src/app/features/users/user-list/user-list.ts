import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { User } from '../../../core/models/user.model';
import { UserService } from '../../../core/services/user.service';

import { TranslocoModule } from '@jsverse/transloco';
import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';

@Component({
  selector: 'app-user-list',
  imports: [
    CommonModule,
    FormsModule,
    TranslocoModule,
    MatTableModule,
    MatInputModule,
    MatFormFieldModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
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
  searchTerm: string = '';
  searchSubject: Subject<string> = new Subject<string>();

  constructor(
    private userService: UserService,
    private router: Router,
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
      },
      error: (err) => console.error('Error fetching users', err),
    });
  }

  manageRole(userId: string): void {
    this.router.navigate(['/admin/users', userId, 'manage-role']);
  }
}
