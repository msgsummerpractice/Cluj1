import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CheckincodesComponent } from './checkincodes-component';

describe('CheckincodesComponent', () => {
  let component: CheckincodesComponent;
  let fixture: ComponentFixture<CheckincodesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CheckincodesComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CheckincodesComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
