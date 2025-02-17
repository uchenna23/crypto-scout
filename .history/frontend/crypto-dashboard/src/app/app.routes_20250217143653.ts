import { Routes } from '@angular/router';
import { PriceChangeDashboardComponent } from './components/price-change-dashboard/price-change-dashboard.component';
import { PriceHistoryDashboardComponent } from './components/price-history-dashboard/price-history-dashboard.component';
import { MarketAnalysisChatComponent } from './components/market-analysis-chat/market-analysis-chat.component';

export const routes: Routes = [
  {
    path: '',
    children: [
      { path: '', component: PriceChangeDashboardComponent, pathMatch: 'full' },
      { path: '', component: PriceHistoryDashboardComponent }
    ]
  },
  { path: 'market-analysis', component: MarketAnalysisChatComponent },
  { path: '**', redirectTo: '' }
];
