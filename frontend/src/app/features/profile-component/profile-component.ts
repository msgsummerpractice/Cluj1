import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  MatCard,
  MatCardContent,
  MatCardHeader,
  MatCardSubtitle,
  MatCardTitle,
} from '@angular/material/card';

import { UserService } from '../../core/services/user.service';
import { UserProfile } from '../../core/models/user-profile.model';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { TranslocoPipe } from '@jsverse/transloco';
import { RegistrationService } from '../../core/services/registration.service';
import { EventService } from '../../core/services/event.service';

@Component({
  selector: 'app-profile-component',
  imports: [
    FormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatCard,
    MatCardContent,
    MatCardHeader,
    MatCardTitle,
    MatCardSubtitle,
    MatSlideToggle,
    TranslocoPipe,
  ],
  templateUrl: './profile-component.html',
  styleUrls: ['./profile-component.css'],
})
export class ProfileComponent implements OnInit {
  private readonly userService: UserService = inject(UserService);
  private readonly registrationService = inject(RegistrationService);
  private readonly eventService: EventService = inject(EventService);

  readonly locations = [
    { value: 'CLUJ', label: 'Cluj-Napoca' },
    { value: 'TIMISOARA', label: 'Timișoara' },
    { value: 'MURES', label: 'Târgu Mureș' },
    { value: 'REMOTE', label: 'Remote' },
  ] as const;

  profile = signal<UserProfile | null>(null);
  saving = signal(false);
  successMessage = signal<string>('');
  errorMessage = signal<string>('');
  previewImage = signal<string | null>(null);
  registrationCount = signal<number | null>(null);
  futureEvents = signal<number | null>(null);

  selectedLocation: string = '';
  selectedFile: File | null = null;

  ngOnInit(): void {
    this.loadProfile();
    this.loadRegistrationCount();
    this.loadUpcomingEventCount();
  }

  loadProfile(): void {
    this.userService.getProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.selectedLocation = profile?.userLocation || '';
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message || 'Failed to load profile.');
      },
    });
  }
  loadRegistrationCount(): void {
    this.registrationService.getRegistrationCountPerUser().subscribe({
      next: (count) => {
        this.registrationCount.set(count);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message || 'Failed to load registration count.');
      },
    });
  }
  loadUpcomingEventCount(): void {
    this.eventService.getUpcomingRegisteredEventsCountPerUser().subscribe({
      next: (count) => {
        this.futureEvents.set(count);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message || 'Failed to load registration count.');
      }
    })
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const allowedTypes = ['image/png', 'image/jpeg', 'image/jpg'];
    if(!allowedTypes.includes(file.type)) {
      this.errorMessage.set("Invalid file type");
      this.clearSelectedFile()
      input.value = '';
      return
    }

    const maxSize = 5*1024*1024;
    if(file.size > maxSize){
      this.errorMessage.set("Maximum size is 5MB");
      this.clearSelectedFile()
      input.value = '';
      return
    }

    this.errorMessage.set('');
    this.selectedFile = file;

    const reader = new FileReader();
    reader.onload = () => this.previewImage.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  clearSelectedFile(): void {
    this.selectedFile = null;
    this.previewImage.set(null);
  }

  updateProfile(): void {
    this.saving.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    this.userService
      .updateProfile(this.selectedLocation, this.selectedFile || undefined)
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.successMessage.set('Profile updated successfully.');
          this.selectedFile = null;
          this.previewImage.set(null);
          this.loadProfile();
        },
        error: (err) => {
          this.saving.set(false);
          this.errorMessage.set(err?.error?.message || err?.message || 'Failed to update profile.');
        },
      });
  }

  displayLocation(value: string | null | undefined): string {
    if (!value) return 'Location not set';
    return this.locations.find((l) => l.value === value)?.label ?? value;
  }
}
