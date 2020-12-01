import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EnvironmentsListComponent } from './environments-list.component';

describe('EnvironmentsListComponent', () => {
  let component: EnvironmentsListComponent;
  let fixture: ComponentFixture<EnvironmentsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ EnvironmentsListComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EnvironmentsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
