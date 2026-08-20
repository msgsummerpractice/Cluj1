import { Component, inject, signal } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslocoModule } from '@jsverse/transloco';

import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-login',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    TranslocoModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly translocoService = inject(TranslocoService);
  private readonly toastService = inject(ToastService);

  protected readonly isSubmitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected hidePassword = true;

  protected readonly loginForm = this.formBuilder.nonNullable.group({
    email: [
      '',
      [Validators.required, Validators.email, Validators.pattern(/^[a-zA-Z0-9._%+-]+@msg\.group$/)],
    ],
    password: ['', [Validators.required]],
  });

  protected submit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const credentials = this.loginForm.getRawValue();
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');

    this.authService
      .login({
        email: credentials.email.trim().toLowerCase(),
        password: credentials.password,
      })
      .pipe(finalize(() => this.isSubmitting.set(false)))
      .subscribe({
        next: (user) => {
          this.toastService.show(
            'success',
            this.translocoService.translate('notifications.confirmLogin', {
              email: credentials.email,
            }),
          );
          const nextUrl = returnUrl || this.authService.getLandingRoute(user.role);
          void this.router.navigateByUrl(nextUrl);
        },
        error: (error) => {
          let errorMsg = 'Invalid email or password.';

          if (error.status === 0) {
            errorMsg = 'Unable to sign in right now. Please try again.';
          }

          this.errorMessage.set(errorMsg);
        },
      });
  }
}
