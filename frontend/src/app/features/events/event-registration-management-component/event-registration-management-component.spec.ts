import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EventRegistrationManagementComponent } from './event-registration-management-component';

describe('EventRegistrationManagementComponent', () => {
  let component: EventRegistrationManagementComponent;
  let fixture: ComponentFixture<EventRegistrationManagementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EventRegistrationManagementComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(EventRegistrationManagementComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
