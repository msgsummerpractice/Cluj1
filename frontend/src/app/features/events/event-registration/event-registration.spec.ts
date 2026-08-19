import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { vi } from 'vitest';

import { EventService } from '../../../core/services/event.service';

import { EventRegistration } from './event-registration';

describe('EventRegistration', () => {
  let component: EventRegistration;
  let fixture: ComponentFixture<EventRegistration>;
  let eventServiceSpy: {
    getEventById: ReturnType<typeof vi.fn>;
    registerForEvent: ReturnType<typeof vi.fn>;
  };

  const baseEvent = {
    id: 'event-1',
    name: 'Internal Event',
    startDate: '2026-09-01T10:00:00Z',
    endDate: '2026-09-01T12:00:00Z',
    registrationEndDate: '2026-08-30T10:00:00Z',
    type: 'INTERNAL' as const,
    location: 'CLUJ' as const,
    status: 'PUBLISHED' as const,
    foodProvided: true,
  };

  beforeEach(async () => {
    eventServiceSpy = {
      getEventById: vi.fn(),
      registerForEvent: vi.fn(),
    };
    eventServiceSpy.getEventById.mockReturnValue(of(baseEvent));
    eventServiceSpy.registerForEvent.mockReturnValue(
      of({ message: 'Successfully registered for the event.' }),
    );

    await TestBed.configureTestingModule({
      imports: [EventRegistration],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'event-1' } } } },
        { provide: Router, useValue: { navigate: vi.fn() } as Pick<Router, 'navigate'> },
        { provide: EventService, useValue: eventServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EventRegistration);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('requires driver details when transportation is needed for internal events', () => {
    component.registrationForm.patchValue({
      transportationNeeded: true,
      driverName: '',
      driverPhone: '',
    });
    component.registrationForm.get('driverName')?.markAsTouched();
    component.registrationForm.get('driverPhone')?.markAsTouched();

    expect(component.registrationForm.get('driverName')?.hasError('required')).toBe(true);
    expect(component.registrationForm.get('driverPhone')?.hasError('required')).toBe(true);
  });

  it('requires accommodation days when accommodation is needed for internal events', () => {
    component.registrationForm.patchValue({ accommodationNeeded: true, accommodationDays: null });
    component.registrationForm.get('accommodationDays')?.markAsTouched();

    expect(component.registrationForm.get('accommodationDays')?.hasError('required')).toBe(true);
  });

  it('removes gdpr requirement for external events', () => {
    component.updateFormValidators({ ...baseEvent, type: 'EXTERNAL' });
    component.registrationForm.patchValue({ gdprConsent: false });

    expect(component.registrationForm.get('gdprConsent')?.errors).toBeNull();
  });
});
