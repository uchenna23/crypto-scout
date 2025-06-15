import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PriceHistoryDashboardComponent } from './price-history-dashboard.component';

describe('PriceHistoryDashboardComponent', () => {
  let component: PriceHistoryDashboardComponent;
  let fixture: ComponentFixture<PriceHistoryDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PriceHistoryDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PriceHistoryDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
