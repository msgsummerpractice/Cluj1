import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { Router } from '@angular/router';
import { EventService } from '../../../core/services/event.service';

import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatFormFieldModule } from '@angular/material/form-field';

@Component({
  selector: 'app-event-creation',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatInputModule,
    MatSelectModule,
    MatRadioModule,
    MatFormFieldModule,
  ],
  templateUrl: './event-creation.html',
  styleUrl: './event-creation.css',
})
export class EventCreationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly eventService = inject(EventService);
  private readonly router = inject(Router);

  readonly locations = ['CLUJ', 'TIMISOARA', 'MURES'];
  readonly selectedFile = signal<File | null>(null);
  readonly fileError = signal<string | null>(null);
  readonly isSubmitting = signal(false);

  form = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    type: ['', Validators.required],
    location: [{ value: '', disabled: true }, Validators.required],
    foodProvided: [{ value: null, disabled: true }],
  });

  ngOnInit() {
    this.form.get('type')?.valueChanges.subscribe((type) => this.handleTypeChange(type));
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
      foodCtrl?.setValue(null);
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

    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        this.fileError.set('File size must be under 5MB.');
        return;
      }
      if (!['image/jpeg', 'image/png'].includes(file.type)) {
        this.fileError.set('Only JPEG/PNG files are allowed.');
        return;
      }
      this.selectedFile.set(file);
    }
  }

  onSubmit() {
    if (this.form.invalid || this.fileError()) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const formValue = this.form.getRawValue();

    this.eventService.createEvent(formValue, this.selectedFile() || undefined).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.router.navigate(['/admin/events']);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        console.error(err);
      },
    });
  }
}
