import { Routes } from '@angular/router';
import { PriceChangeDashboardComponent } from './components/price-change-dashboard/price-change-dashboard.component';
import { MarketAnalysisChatComponent } from './components/market-analysis-chat/market-analysis-chat.component';

export const routes: Routes = [
  { path: 'dashboards', component: PriceChangeDashboardComponent },
  { path: 'market analysis', component: MarketAnalysisChatComponent },
  { path: '**', redirectTo: 'dashboards' }
];
