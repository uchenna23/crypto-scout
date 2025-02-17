import { Component } from '@angular/core';
import { CryptoDashboardComponent } from './pages/dashboard/dashboard.component';
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CryptoDashboardComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'crypto-scout';
}
