import { Routes } from '@angular/router';
import { PriceChangeDashboardComponent } from './components/price-change-dashboard/price-change-dashboard.component';
import { PriceHistoryDashboardComponent } from './components/price-history-dashboard/price-history-dashboard.component';
import { MarketAnalysisChatComponent } from './components/market-analysis-chat/market-analysis-chat.component';
import { CommonModule } from '@angular/common';

export const routes: Routes = [
  { 
    path: '', 
    component: PriceChangeDashboardComponent 
  },
  { 
    path: '', 
    component: PriceHistoryDashboardComponent 
  },
  { 
    path: 'market-analysis', 
    component: MarketAnalysisChatComponent 
  },
  { 
    path: '**', 
    redirectTo: '' 
  } // Redirects unknown routes to home
];
