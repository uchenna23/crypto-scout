import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MarketAnalysisChatComponent } from './market-analysis-chat.component';

describe('MarketAnalysisChatComponent', () => {
  let component: MarketAnalysisChatComponent;
  let fixture: ComponentFixture<MarketAnalysisChatComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MarketAnalysisChatComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MarketAnalysisChatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
