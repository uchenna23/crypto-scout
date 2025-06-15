import { Routes } from '@angular/router';
import { MarketAnalysisChatComponent } from './components/market-analysis-chat/market-analysis-chat.component';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboards', pathMatch: 'full' },
  { path: 'dashboards', redirectTo: '', pathMatch: 'full' },
  { path: 'market-analysis', component: MarketAnalysisChatComponent },
  { path: '**', redirectTo: 'dashboards' }
];
