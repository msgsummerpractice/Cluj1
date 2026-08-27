import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EventService } from '../../../core/services/event.service';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatTimepickerModule } from '@angular/material/timepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ToastService } from '../../../core/services/toast.service';
import { BackButtonComponent } from '../../../shared/components/back-button/back-button';
import { QuillModule } from 'ngx-quill';

@Component({
  selector: 'app-event-creation',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslocoModule,
    BackButtonComponent,
    MatButtonModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatIconModule,
    MatFormFieldModule,
    MatDatepickerModule,
    MatTimepickerModule,
    MatNativeDateModule,
    MatTooltipModule,
    QuillModule,
  ],
  templateUrl: './event-creation.html',
  styleUrl: './event-creation.css',
})
export class EventCreationComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly eventService = inject(EventService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly translocoService = inject(TranslocoService);
  private readonly toastService = inject(ToastService);

  readonly locations = ['CLUJ', 'TIMISOARA', 'MURES'];
  readonly selectedFile = signal<File | null>(null);
  readonly filePreview = signal<string | null>(null);
  readonly fileError = signal<string | null>(null);
  readonly isSubmitting = signal(false);

  readonly eventId = signal<string | null>(null);
  readonly isEditMode = computed(() => this.eventId() !== null);
  private existingPosterUrl: string | null = null;

  readonly descriptionModules = {
    toolbar: [
      ['bold', 'italic', 'underline', 'strike'],
      [{ color: [] }, { background: [] }],
      [{ list: 'ordered' }, { list: 'bullet' }],
      ['link'],
      ['clean'],
    ],
  };

  form = this.fb.group(
    {
      name: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(10000)]],
      date: [<Date | string | null>'', Validators.required],
      startTime: [<Date | string | null>'', Validators.required],
      endTime: [<Date | string | null>'', Validators.required],
      type: ['', Validators.required],
      location: [{ value: '', disabled: true }, Validators.required],
      foodProvided: [{ value: false, disabled: true }],
      registrationEndDate: [<Date | string | null>null],
    },
    { validators: [endAfterStart, regEndBeforeStart] },
  );

  ngOnInit() {
    this.form.get('type')?.valueChanges.subscribe((type) => this.handleTypeChange(type));

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.eventId.set(id);
      this.loadEventData(id);
    }
  }

  ngOnDestroy() {
    if (this.existingPosterUrl) {
      URL.revokeObjectURL(this.existingPosterUrl);
    }
  }

  loadEventData(id: string) {
    this.eventService.getEventById(id).subscribe({
      next: (event) => {
        const startDate = new Date(event.startDate);
        const endDate = new Date(event.endDate);

        this.form.patchValue({
          name: event.name,
          description: event.description || '',
          date: startDate,
          startTime: startDate,
          endTime: endDate,
          type: event.type,
          location: event.location,
          foodProvided: event.foodProvided || false,
          registrationEndDate: event.registrationEndDate
            ? new Date(event.registrationEndDate)
            : null,
        });

        this.handleTypeChange(event.type);
        this.loadExistingPoster(id);
      },
      error: (err) => console.error('Error loading event for edit', err),
    });
  }

  private loadExistingPoster(id: string) {
    this.eventService.getEventPoster(id).subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) return;
        const url = URL.createObjectURL(blob);
        this.existingPosterUrl = url;
        this.filePreview.set(url);
      },
      error: () => {
        // No poster for this event, keep default upload placeholder
      },
    });
  }

  handleTypeChange(type: string | null) {
    const locationCtrl = this.form.get('location');
    const foodCtrl = this.form.get('foodProvided');

    if (type === 'INTERNAL') {
      locationCtrl?.setValue('ALL');
      locationCtrl?.disable();
      foodCtrl?.enable();
    } else if (type === 'EXTERNAL') {
      if (locationCtrl?.value === 'ALL') locationCtrl?.setValue(null);
      locationCtrl?.enable();
      foodCtrl?.setValue(false);
      foodCtrl?.disable();
    } else if (type === 'LOCAL') {
      if (locationCtrl?.value === 'ALL') locationCtrl?.setValue(null);
      locationCtrl?.enable();
      foodCtrl?.enable();
    }
  }

  onFileChange(event: any) {
    const file = event.target.files[0];
    this.fileError.set(null);
    this.selectedFile.set(null);
    if (this.existingPosterUrl) {
      URL.revokeObjectURL(this.existingPosterUrl);
      this.existingPosterUrl = null;
    }
    this.filePreview.set(null);

    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        this.fileError.set(this.translocoService.translate('createEvent.errors.fileSize'));
        return;
      }
      if (!['image/jpeg', 'image/png'].includes(file.type)) {
        this.fileError.set(this.translocoService.translate('createEvent.errors.fileType'));
        return;
      }
      this.selectedFile.set(file);

      const reader = new FileReader();
      reader.onload = () => {
        this.filePreview.set(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  }

  discard() {
    this.router.navigate(['/events']);
  }

  clearRegistrationEndDate(event: Event) {
    event.stopPropagation();
    this.form.get('registrationEndDate')?.setValue(null);
  }

  onSubmit() {
    if (this.form.invalid || this.fileError()) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const formValue = this.form.getRawValue() as any;
    const dateObj = new Date(formValue.date);

    const startDate = new Date(dateObj);
    if (formValue.startTime instanceof Date) {
      startDate.setHours(formValue.startTime.getHours(), formValue.startTime.getMinutes(), 0);
    } else if (typeof formValue.startTime === 'string') {
      const [startHour, startMin] = formValue.startTime.split(':');
      startDate.setHours(Number(startHour), Number(startMin), 0);
    }

    const endDate = new Date(dateObj);
    if (formValue.endTime instanceof Date) {
      endDate.setHours(formValue.endTime.getHours(), formValue.endTime.getMinutes(), 0);
    } else if (typeof formValue.endTime === 'string') {
      const [endHour, endMin] = formValue.endTime.split(':');
      endDate.setHours(Number(endHour), Number(endMin), 0);
    }

    const payload = {
      name: formValue.name,
      description: formValue.description,
      type: formValue.type,
      location: formValue.location,
      foodProvided: formValue.foodProvided ? true : false,
      startDate: startDate.toISOString(),
      endDate: endDate.toISOString(),
      registrationEndDate: formValue.registrationEndDate
        ? new Date(formValue.registrationEndDate).toISOString()
        : null,
    };

    const id = this.eventId();

    const request$ = id
      ? this.eventService.updateEvent(id, payload, this.selectedFile() || undefined)
      : this.eventService.createEvent(payload, this.selectedFile() || undefined);

    request$.subscribe({
      next: (result: any) => {
        this.isSubmitting.set(false);
        this.toastService.show(
          'success',
          this.translocoService.translate(id ? 'createEvent.updated' : 'createEvent.created'),
        );
        if (id) {
          this.router.navigate(['/events', id]);
        } else {
          // Stay on the form in edit mode for the newly created event
          this.router.navigate(['/events', result.id, 'edit'], { replaceUrl: true });
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.show(
          'error',
          err?.error?.message || err?.message || 'Failed to create event.',
        );
      },
    });
  }
}

function buildDateTime(date: unknown, time: unknown): Date | null {
  if (!date || !time) return null;
  const d = new Date(date as string | Date);
  if (isNaN(d.getTime())) return null;
  if (time instanceof Date) {
    d.setHours(time.getHours(), time.getMinutes(), 0, 0);
  } else if (typeof time === 'string' && time.includes(':')) {
    const [h, m] = time.split(':');
    d.setHours(Number(h), Number(m), 0, 0);
  } else {
    return null;
  }
  return d;
}

function endAfterStart(group: AbstractControl): ValidationErrors | null {
  const { date, startTime, endTime } = group.value;
  const start = buildDateTime(date, startTime);
  const end = buildDateTime(date, endTime);
  if (!start || !end) return null;
  return end <= start ? { endBeforeStart: true } : null;
}

function regEndBeforeStart(group: AbstractControl): ValidationErrors | null {
  const { date, registrationEndDate } = group.value;
  if (!date || !registrationEndDate) return null;
  const eventStart = new Date(date as string | Date);
  if (isNaN(eventStart.getTime())) return null;
  eventStart.setHours(0, 0, 0, 0);
  const regEnd = new Date(registrationEndDate as string | Date);
  if (isNaN(regEnd.getTime())) return null;
  regEnd.setHours(23, 59, 59, 999);
  return regEnd >= eventStart ? { regEndAfterStart: true } : null;
}
