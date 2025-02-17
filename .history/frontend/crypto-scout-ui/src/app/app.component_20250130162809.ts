import { Component } from '@angular/core';
import { CryptoDashboardComponent } from './pages/dashboard/dashboard.component';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CryptoDashboardComponent, RouterModule], // ✅ Import RouterModule
  templateUrl: './app.component.html',
})
export class AppComponent {}
