import { Routes } from '@angular/router';
import { PriceChangeDashboardComponent } from './components/price-change-dashboard/price-change-dashboard.component';

export const routes: Routes = [
  { path: 'dashboards', component: PriceChangeDashboardComponent },
  { path: '**', redirectTo: 'dashboards' }
];
