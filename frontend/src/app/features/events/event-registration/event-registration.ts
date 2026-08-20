import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { BackButtonComponent } from '../../../shared/components/back-button/back-button';
import {
  PhoneInput,
  PHONE_NUMBER_PATTERN,
} from '../../../shared/components/phone-input/phone-input';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { EventService } from '../../../core/services/event.service';
import { Event } from '../../../core/models/event.model';
import { EventRegistrationRequest } from '../../../core/models/event-registration.model';
import { TranslocoModule } from '@jsverse/transloco';

import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-event-registration-view',
  standalone: true,
  imports: [
    BackButtonComponent,
    CommonModule,
    ReactiveFormsModule,
    TranslocoModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatIconModule,
    PhoneInput,
  ],
  templateUrl: './event-registration.html',
  styleUrl: './event-registration.css',
})
export class EventRegistration implements OnInit {
  readonly event = signal<Event | null>(null);
  readonly isRegistrationClosed = signal<boolean>(false);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly isSubmitting = signal<boolean>(false);
  readonly isRegistered = signal<boolean>(false);

  eventId!: string;
  registrationForm!: FormGroup;

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private eventService = inject(EventService);
  private fb = inject(FormBuilder);

  ngOnInit(): void {
    this.eventId = this.route.snapshot.paramMap.get('id')!;

    this.initEmptyForm();
    this.bindConditionalFieldValidation();

    this.eventService.getEventById(this.eventId).subscribe({
      next: (eventData) => {
        this.event.set(eventData);

        if (eventData?.registrationEndDate) {
          const regEndDate = new Date(eventData.registrationEndDate).getTime();
          this.isRegistrationClosed.set(Date.now() > regEndDate);
        }

        this.updateFormValidators(eventData);
      },
      error: () => {
        this.errorMessage.set('Could not load event details.');
      },
    });

    this.eventService.checkIfAlreadyRegistered(this.eventId).subscribe({
      next: (isRegistered) => {
        if (isRegistered) {
          this.isRegistered.set(true);
        }
      },
    });
  }

  initEmptyForm(): void {
    this.registrationForm = this.fb.group({
      gdprConsent: [false, [Validators.requiredTrue]],
      photoConsent: [true],
      foodPreference: ['NONE'],
      transportationNeeded: [false],
      driverName: [''],
      driverPhone: [''],
      accommodationNeeded: [false],
      accommodationDays: [null, [Validators.min(1)]],
    });
  }

  updateFormValidators(eventData: Event): void {
    const isExternal = eventData?.type === 'EXTERNAL';
    const gdprControl =
      this.registrationForm.get('gdprControl') || this.registrationForm.get('gdprConsent');
    const photoConsentControl = this.registrationForm.get('photoConsent');

    if (isExternal) {
      gdprControl?.clearValidators();
    } else {
      gdprControl?.setValidators([Validators.requiredTrue]);
    }
    gdprControl?.updateValueAndValidity();
    photoConsentControl?.setValue(true);
    photoConsentControl?.clearValidators();
    photoConsentControl?.updateValueAndValidity();

    this.syncConditionalFieldValidators(eventData);
  }

  private toastService = inject(ToastService);
  onSubmit(): void {
    const photoConsentControl = this.registrationForm.get('photoConsent');
    if (photoConsentControl) {
      photoConsentControl.setValue(true);
    }

    if (this.registrationForm.invalid || this.isSubmitting() || this.isRegistered()) {
      this.registrationForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    const requestData: EventRegistrationRequest = this.registrationForm.value;

    this.eventService.registerForEvent(this.eventId, requestData).subscribe({
      next: () => {
        this.toastService.show('success', 'Successfully registered for the event!');
        this.errorMessage.set(null);
        this.isSubmitting.set(false);
        this.isRegistered.set(true);
      },
      error: (err) => {
        const message =
          typeof err.error === 'string'
            ? err.error
            : err.error?.message || 'An error occurred during registration.';
        this.errorMessage.set(message);
        this.toastService.show('error', message);
        this.isSubmitting.set(false);
      },
    });
  }

  private bindConditionalFieldValidation(): void {
    this.registrationForm.get('transportationNeeded')?.valueChanges.subscribe(() => {
      this.syncConditionalFieldValidators(this.event());
    });

    this.registrationForm.get('accommodationNeeded')?.valueChanges.subscribe(() => {
      this.syncConditionalFieldValidators(this.event());
    });
  }

  private syncConditionalFieldValidators(eventData: Event | null): void {
    // matches the template gating: this section shows for any non-EXTERNAL event
    const isInternal = eventData?.type !== 'EXTERNAL';
    const transportationNeeded = this.registrationForm.get('transportationNeeded')?.value === true;
    const accommodationNeeded = this.registrationForm.get('accommodationNeeded')?.value === true;
    const driverNameControl = this.registrationForm.get('driverName');
    const driverPhoneControl = this.registrationForm.get('driverPhone');
    const accommodationDaysControl = this.registrationForm.get('accommodationDays');

    if (!isInternal) {
      this.registrationForm.patchValue(
        {
          transportationNeeded: false,
          driverName: '',
          driverPhone: '',
          accommodationNeeded: false,
          accommodationDays: null,
        },
        { emitEvent: false },
      );
    }

    driverNameControl?.setValidators(
      isInternal && transportationNeeded ? [Validators.required] : [],
    );
    driverPhoneControl?.setValidators(
      isInternal && transportationNeeded
        ? [Validators.required, Validators.pattern(PHONE_NUMBER_PATTERN)]
        : [],
    );
    accommodationDaysControl?.setValidators(
      isInternal && accommodationNeeded ? [Validators.required, Validators.min(1)] : [],
    );

    if (!(isInternal && transportationNeeded)) {
      driverNameControl?.setValue('', { emitEvent: false });
      driverPhoneControl?.setValue('', { emitEvent: false });
    }

    if (!(isInternal && accommodationNeeded)) {
      accommodationDaysControl?.setValue(null, { emitEvent: false });
    }

    driverNameControl?.updateValueAndValidity({ emitEvent: false });
    driverPhoneControl?.updateValueAndValidity({ emitEvent: false });
    accommodationDaysControl?.updateValueAndValidity({ emitEvent: false });
  }
}
