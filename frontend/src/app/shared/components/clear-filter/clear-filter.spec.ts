import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ClearFilter } from './clear-filter';

describe('ClearFilter', () => {
  let component: ClearFilter;
  let fixture: ComponentFixture<ClearFilter>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClearFilter],
    }).compileComponents();

    fixture = TestBed.createComponent(ClearFilter);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
