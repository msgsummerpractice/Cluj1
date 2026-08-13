import { Component, inject } from '@angular/core';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth/auth-service';
import { Router } from '@angular/router';
import {ChangeDetectorRef} from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
@Component({
  selector: 'app-register-component',
  imports: [
    ReactiveFormsModule,
    TranslocoModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
  ],
  templateUrl: './register-component.html',
  styleUrls: [],
})
export class RegisterComponent {
  private readonly _formBuilder: NonNullableFormBuilder = inject(NonNullableFormBuilder);
  private readonly authService: AuthService = inject(AuthService);
  private readonly router: Router = inject(Router);
  private readonly cdr: ChangeDetectorRef = inject(ChangeDetectorRef);

  formMessage: string = '';

  registerForm = this._formBuilder.group(
    {
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      userLocation: ['', [Validators.required]],
      email: [
        '',
        [
          Validators.required,
          Validators.pattern('^[a-zA-Z0-9]+\\.[a-zA-Z0-9]+@msg.group'),
        ],
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
      console.log(firstName, lastName, userLocation, email, password, confirmPassword);
      this.authService
        .registerUser({ firstName, lastName, userLocation, email, password, confirmPassword })
        .subscribe({
          next: () => {
            this.formMessage = 'User registered successfully!';
            this.cdr.detectChanges();
            this.registerForm.reset();
            setTimeout(() => {
              this.router.navigate(['/login']);
            }, 1500);
          },
          error: (err) => {
            const errorMessage =
              err.error?.error || err.error || 'An error occurred during registration.';
            this.formMessage = errorMessage;
            this.cdr.detectChanges();
            console.error(err);
          },
        });
    }
  }
}
