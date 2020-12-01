import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EnvironmentsEditComponent } from './environments-edit.component';

describe('EnvironmentsEditComponent', () => {
  let component: EnvironmentsEditComponent;
  let fixture: ComponentFixture<EnvironmentsEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ EnvironmentsEditComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EnvironmentsEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
