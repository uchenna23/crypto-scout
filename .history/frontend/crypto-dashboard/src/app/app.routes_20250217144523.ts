import { Routes } from '@angular/router';
import { MarketAnalysisChatComponent } from './components/market-analysis-chat/market-analysis-chat.component';

export const routes: Routes = [
  { 
    path: 'market-analysis', 
    component: MarketAnalysisChatComponent 
  },
  { 
    path: '**', 
    redirectTo: '' 
  } // Redirects unknown routes to home
];
