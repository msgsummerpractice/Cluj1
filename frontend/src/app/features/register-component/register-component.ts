import { Component, inject } from '@angular/core';
import { AbstractControl, FormGroup, NonNullableFormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth/auth-service';

@Component({
  selector: 'app-register-component',
  imports: [],
  templateUrl: './register-component.html',
  styleUrl: './register-component.css',
})
export class RegisterComponent {
  private readonly _formBuilder: NonNullableFormBuilder = inject(NonNullableFormBuilder);
  private readonly authService: AuthService = inject(AuthService);

  succesMessage: string = '';
  errorMessage: string = '';

  registerForm = this._formBuilder.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]],
  },{validators: this.passwordMatchValidator})

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if(password !== confirmPassword) {
      control.get('confirmPassword')?.setErrors({passwordMismatch: true });
      return {passwordMismatch: true};
    }
    return null;
  }

  onSubmit() {
    if(this.registerForm.valid) {
      const { email, password } = this.registerForm.getRawValue();
      this.authService.registerUser({email, password}).subscribe({
        next: () => {
          this.succesMessage = 'User registered successfully!'
          this.registerForm.reset();
        },
        error: (err) => {
          this.errorMessage = 'Error registering user';
          console.error(err);
        }
      })
    }
  }


}
