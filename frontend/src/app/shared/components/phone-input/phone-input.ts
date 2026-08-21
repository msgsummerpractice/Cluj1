import { Component, input, Optional, Self, signal } from '@angular/core';
import { ControlValueAccessor, NgControl, ReactiveFormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export const PHONE_NUMBER_PATTERN = /^\+?[0-9]+$/;

@Component({
  selector: 'app-phone-input',
  standalone: true,
  imports: [ReactiveFormsModule, TranslocoModule, MatFormFieldModule, MatInputModule],
  templateUrl: './phone-input.html',
})
export class PhoneInput implements ControlValueAccessor {
  readonly label = input('eventRegistration.driverPhone');
  readonly placeholder = input('eventRegistration.driverPhonePlaceholder');
  readonly errorLabel = input('eventRegistration.driverPhoneError');
  readonly patternErrorLabel = input('eventRegistration.driverPhonePatternError');

  value = signal('');
  disabled = signal(false);

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(@Optional() @Self() public ngControl: NgControl) {
    if (this.ngControl) {
      this.ngControl.valueAccessor = this;
    }
  }

  get invalid(): boolean {
    return !!this.ngControl?.invalid;
  }

  get patternInvalid(): boolean {
    return !!this.ngControl?.hasError('pattern');
  }

  get touched(): boolean {
    return !!this.ngControl?.touched;
  }

  writeValue(value: string | null | undefined): void {
    this.value.set(value ?? '');
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  onInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.value.set(value);
    this.onChange(value);
  }

  markTouched(): void {
    this.onTouched();
  }
}
