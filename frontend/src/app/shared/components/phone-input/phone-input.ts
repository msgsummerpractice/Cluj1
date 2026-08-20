import { Component, input, Optional, Self } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NgControl, ReactiveFormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export const PHONE_NUMBER_PATTERN = /^\+?[0-9]+$/;

@Component({
  selector: 'app-phone-input',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule, MatFormFieldModule, MatInputModule],
  templateUrl: './phone-input.html',
})
export class PhoneInput implements ControlValueAccessor {
  readonly label = input('eventRegistration.driverPhone');
  readonly placeholder = input('eventRegistration.driverPhonePlaceholder');
  readonly errorLabel = input('eventRegistration.driverPhoneError');
  readonly patternErrorLabel = input('eventRegistration.driverPhonePatternError');

  value = '';
  disabled = false;

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(@Optional() @Self() private ngControl: NgControl) {
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

  writeValue(value: string): void {
    this.value = value ?? '';
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  onInput(value: string): void {
    this.value = value;
    this.onChange(value);
  }

  markTouched(): void {
    this.onTouched();
  }
}
