import { Routes } from '@angular/router';
import { DashboardComponent } from './components/price-change-dashboard/price-change-dashboard.component';

export const routes: Routes = [
  { path: 'dashboards', component: DashboardComponent },
  { path: '**', redirectTo: 'dashboards' }
];
