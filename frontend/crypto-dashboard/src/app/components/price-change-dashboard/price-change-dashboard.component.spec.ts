import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PriceChangeDashboardComponent } from './price-change-dashboard.component';

describe('PriceChangeDashboardComponent', () => {
  let component: PriceChangeDashboardComponent;
  let fixture: ComponentFixture<PriceChangeDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PriceChangeDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PriceChangeDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
