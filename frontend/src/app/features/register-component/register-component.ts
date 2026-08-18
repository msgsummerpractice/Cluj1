import { Component, inject } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { ChangeDetectorRef } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { UserService } from '../../core/services/user.service';
import {
  MatCard,
  MatCardContent,
  MatCardHeader,
  MatCardSubtitle,
  MatCardTitle,
} from '@angular/material/card';
import { RouterLink } from '@angular/router';
import { ToastService } from '../../core/services/toast.service';
@Component({
  selector: 'app-register-component',
  imports: [
    ReactiveFormsModule,
    TranslocoModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCardContent,
    MatCardSubtitle,
    MatCardTitle,
    MatCard,
    MatCardHeader,
    RouterLink,
  ],
  templateUrl: './register-component.html',
  styleUrls: ['./register-component.css'],
})
export class RegisterComponent {
  private readonly _formBuilder: NonNullableFormBuilder = inject(NonNullableFormBuilder);
  private readonly userService: UserService = inject(UserService);
  private readonly router: Router = inject(Router);
  private readonly cdr: ChangeDetectorRef = inject(ChangeDetectorRef);
  private readonly toastService: ToastService = inject(ToastService);

  readonly locations = [
    { value: 'CLUJ', labelKey: 'register.locations.cluj' },
    { value: 'TIMISOARA', labelKey: 'register.locations.timisoara' },
    { value: 'MURES', labelKey: 'register.locations.mures' },
    { value: 'REMOTE', labelKey: 'register.locations.remote' },
  ] as const;

  hidePassword = true;
  hideConfirmPassword = true;

  registerForm = this._formBuilder.group(
    {
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      userLocation: ['', [Validators.required]],
      email: [
        '',
        [Validators.required, Validators.pattern('^[a-zA-Z0-9]+\\.[a-zA-Z0-9]+@msg.group')],
      ],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.pattern('^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$'),
        ],
      ],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: this.passwordMatchValidator },
  );

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if (password !== confirmPassword) {
      control.get('confirmPassword')?.setErrors({ passwordMismatch: true });
      return { passwordMismatch: true };
    }
    return null;
  }

  onSubmit() {
    if (this.registerForm.valid) {
      const { firstName, lastName, userLocation, email, password, confirmPassword } =
        this.registerForm.getRawValue();
      this.userService
        .registerUser({ firstName, lastName, userLocation, email, password, confirmPassword })
        .subscribe({
          next: () => {
            this.toastService.show('success', 'User registered successfully!');
            this.cdr.detectChanges();
            this.registerForm.reset();
            this.router.navigate(['/login']);
          },
          error: (err) => {
            const errorMessage =
              err.error?.message ||
              err.error?.error ||
              err.message ||
              'An error occurred during registration.';
            this.toastService.show('error', errorMessage);
            this.cdr.detectChanges();
          },
        });
    }
  }
}
