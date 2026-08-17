import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../../core/services/auth.service';
import { TranslocoModule } from '@jsverse/transloco';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    TranslocoModule,
  ],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css',
})
export class ResetPasswordComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);

  protected hidePassword = true;
  protected hideConfirmPassword = true;
  private token: string | null = null;

  protected readonly resetForm = this.formBuilder.nonNullable.group({
    newPassword: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[^A-Za-z0-9]).+$/),
      ],
    ],
    confirmPassword: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');
    if (!this.token) {
      this.errorMessage.set('Invalid or missing password reset token.');
    }
  }

  protected submit(): void {
    if (this.resetForm.invalid || !this.token) {
      this.resetForm.markAllAsTouched();
      return;
    }

    const { newPassword, confirmPassword } = this.resetForm.getRawValue();

    if (newPassword !== confirmPassword) {
      this.errorMessage.set('Passwords do not match.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    this.authService
      .resetPassword({
        token: this.token,
        newPassword,
        confirmPassword,
      })
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (msg) => {
          this.successMessage.set(msg);
          setTimeout(() => {
            void this.router.navigate(['/login']);
          }, 3000);
        },
        error: (err) => {
          this.errorMessage.set(
            err.error || 'Failed to reset password. The link might be expired.',
          );
        },
      });
  }
}
