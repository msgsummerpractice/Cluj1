import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RubiksCubeComponent } from './rubiks-cube-component';

describe('RubiksCubeComponent', () => {
  let component: RubiksCubeComponent;
  let fixture: ComponentFixture<RubiksCubeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RubiksCubeComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(RubiksCubeComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
