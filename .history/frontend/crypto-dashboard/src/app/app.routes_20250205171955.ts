import { Routes } from '@angular/router';
import { DashboardComponent } from './components/price-change-dashboard/price-change-dashboard.component';

export const routes: Routes = [  // ✅ Make sure it's exported
  { path: 'dashboards', component: DashboardComponent },
  { path: '**', redirectTo: 'dashboards' }
];
