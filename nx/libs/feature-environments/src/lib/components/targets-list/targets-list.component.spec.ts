import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TargetsListComponent } from './targets-list.component';

describe('TargetsListComponent', () => {
  let component: TargetsListComponent;
  let fixture: ComponentFixture<TargetsListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ TargetsListComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(TargetsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
