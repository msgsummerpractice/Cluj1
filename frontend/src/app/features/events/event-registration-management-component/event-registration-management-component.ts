import { MatDialog } from '@angular/material/dialog';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog.model';
import { filter } from 'rxjs/operators';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { EventService } from '../../../core/services/event.service';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastService } from '../../../core/services/toast.service';
import { Event } from '../../../core/models/event.model';
import { EventRegistrationRequest } from '../../../core/models/event-registration.model';
import { MatIconModule } from '@angular/material/icon';
import{ MatFormFieldModule } from '@angular/material/form-field';
import { MatOption, MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { BackButtonComponent } from '../../../shared/components/back-button/back-button';
import {MatInputModule} from '@angular/material/input';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-event-registration-management',
  imports: [
    ReactiveFormsModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatOption,
    MatSlideToggleModule,
    BackButtonComponent,
    MatInputModule,
    TranslocoPipe,
  ],
  templateUrl: './event-registration-management-component.html',
  styleUrl: './event-registration-management-component.css',
})
export class EventRegistrationManagement {
  private fb = inject(FormBuilder);
  private eventService = inject(EventService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private toast = inject(ToastService);
  private dialog = inject(MatDialog);

  event = signal<Event | null>(null);
  registrationForm!: FormGroup;
  isSubmitting = signal<boolean>(false);
  isDeleting = signal<boolean>(false);

  eventId = '';

  ngOnInit(): void {
    this.eventId = this.route.snapshot.paramMap.get('id') || '';

    this.initForm();
    this.loadEventData();
    this.setupConditionalValidation();
  }

  private initForm(): void {
    this.registrationForm = this.fb.group({
      foodPreference: ['NONE'],
      transportationNeeded: [false],
      driverName: [''],
      driverPhone: [''],
      accommodationNeeded: [false],
      accommodationDays: [null],
      gdprConsent: [true],
      photoConsent: [false],
    });
  }
  private setupConditionalValidation(): void {
    this.registrationForm.get('transportationNeeded')?.valueChanges.subscribe((needed) => {
      const nameControl = this.registrationForm.get('driverName');
      const phoneControl = this.registrationForm.get('driverPhone');

      if (needed) {
        nameControl?.setValidators([Validators.required]);
        phoneControl?.setValidators([Validators.required]);
      } else {
        nameControl?.clearValidators();
        phoneControl?.clearValidators();
      }
      nameControl?.updateValueAndValidity();
      phoneControl?.updateValueAndValidity();
    });
    this.registrationForm.get('accommodationNeeded')?.valueChanges.subscribe((needed) => {
      const daysControl = this.registrationForm.get('accommodationDays');
      if (needed) {
        daysControl?.setValidators([Validators.required, Validators.min(1)]);
      } else {
        daysControl?.clearValidators();
      }
      daysControl?.updateValueAndValidity();
    });
  }
  private loadEventData(): void {
    this.eventService.getEventById(this.eventId).subscribe({
      next: (ev) => {
        this.event.set(ev);
      },
      error: (err) => this.toast.show('error', err),
    });

    this.eventService.getRegistrationDetails(this.eventId).subscribe({
      next: (registrationData: EventRegistrationRequest) => {
        this.registrationForm.patchValue({
          foodPreference: registrationData.foodPreference || 'NONE',
          transportationNeeded: registrationData.transportationNeeded || false,
          accommodationNeeded: registrationData.accommodationNeeded || false,
          accommodationDays: registrationData.accommodationDays || null,
          gdprConsent: registrationData.gdprConsent || false,
          photoConsent: registrationData.photoConsent || false,
          driverName: registrationData.driverName || '',
          driverPhone: registrationData.driverPhone || '',
        });
      },
    });
  }
  onSubmit(): void {
    if (this.registrationForm.invalid) {
      this.registrationForm.markAllAsTouched();
      return;
    }

    const formValues = this.registrationForm.value;

    if (formValues.gdprConsent === false) {
      this.openConfirmDialog({
        titleKey: 'Revoke GDPR Consent?',
        messageKey:
          'Removing your GDPR consent will permanently cancel your registration. Are you sure you want to proceed?',
        confirmKey: 'Yes, Cancel Registration',
        cancelKey: 'Go Back',
      })
        .pipe(filter((confirmed): confirmed is true => Boolean(confirmed)))
        .subscribe(() => {
          this.performUpdate(formValues);
        });
    } else {
      this.performUpdate(formValues);
    }
  }

  private performUpdate(formValues: any): void {
    this.isSubmitting.set(true);

    this.eventService.updateRegistration(this.eventId, formValues).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        if (res && res.message) {
          this.toast.show('success', res.message);
        } else {
          this.toast.show('success', 'Registration updated successfully!');
        }
        setTimeout(() => this.router.navigate(['/events']), 1500);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toast.show('error', err.error?.message || 'Failed to update registration.');
      },
    });
  }

  onDelete(): void {
    this.openConfirmDialog({
      titleKey: 'Cancel Registration?',
      messageKey:
        'Are you sure you want to cancel your registration for this event? This action cannot be undone.',
      confirmKey: 'Yes, Cancel Registration',
      cancelKey: 'Go Back',
    })
      .pipe(filter((confirmed): confirmed is true => Boolean(confirmed)))
      .subscribe(() => {
        this.performDelete();
      });
  }

  private performDelete(): void {
    this.isDeleting.set(true);

    this.eventService.deleteRegistration(this.eventId).subscribe({
      next: () => {
        this.isDeleting.set(false);
        this.toast.show('success', 'Registration deleted successfully.');
        setTimeout(() => this.router.navigate(['/events']), 1500);
      },
      error: (err) => {
        this.isDeleting.set(false);
        this.toast.show('error', err.error || 'Failed to delete registration.');
      },
    });
  }

  private openConfirmDialog(data: ConfirmDialogData) {
    return this.dialog
      .open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
        width: '400px',
        data,
      })
      .afterClosed();
  }
}
