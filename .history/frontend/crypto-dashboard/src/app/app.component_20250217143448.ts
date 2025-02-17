import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { PriceChangeDashboardComponent } from './components/price-change-dashboard/price-change-dashboard.component';
import { PriceHistoryDashboardComponent } from './components/price-history-dashboard/price-history-dashboard.component'; 
import { MenubarModule } from 'primeng/menubar';
import { ButtonModule } from 'primeng/button'; 
import { CommonModule } from '@angular/common';
import { MarketAnalysisChatComponent } from './components/market-analysis-chat/market-analysis-chat.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [PriceChangeDashboardComponent, MenubarModule, ButtonModule, CommonModule, PriceHistoryDashboardComponent, MarketAnalysisChatComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'Crypto Scout';
}
