import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';

export const routes: Routes = [  // ✅ Make sure it's exported
  { path: '', component: DashboardComponent },
  { path: '**', redirectTo: '' }
];
