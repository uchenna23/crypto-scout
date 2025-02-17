import { Routes } from '@angular/router';
import { PriceChangeDashboardComponent } from './components/price-change-dashboard/price-change-dashboard.component';
import { PriceHistoryDashboardComponent } from './components/price-history-dashboard/price-history-dashboard.component';
import { MarketAnalysisChatComponent } from './components/market-analysis-chat/market-analysis-chat.component';
import { CommonModule } from '@angular/common';

export const routes: Routes = [
  {
    path: '',
    children: [
      { path: '', component: PriceChangeDashboardComponent, pathMatch: 'full' }, // Main dashboard
      { path: '', component: PriceHistoryDashboardComponent } // Include both dashboards
    ]
  },
  { path: 'market-analysis', component: MarketAnalysisChatComponent },
  { path: '**', redirectTo: '' } // Redirect invalid paths to home
];
