import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CryptoDashboardComponent } from './dashboard.component';
import { CryptoChartComponent } from '../crypto-chart/crypto-chart.component';

describe('DashboardComponent', () => {
  let component: CryptoDashboardComponent;
  let fixture: ComponentFixture<CryptoDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CryptoDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CryptoDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
