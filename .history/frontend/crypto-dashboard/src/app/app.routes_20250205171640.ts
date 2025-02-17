import { Routes } from '@angular/router';
import { DashboardComponent } from './components/lprice-change-dashboard/dashboard.component';

export const routes: Routes = [  // ✅ Make sure it's exported
  { path: 'dashboard', component: DashboardComponent },
  { path: '**', redirectTo: 'dashboard' }
];
