import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { DashboardComponent } from './components/price-change-dashboard/price-change-dashboard.component';
import { MenubarModule } from 'primeng/menubar';        // ✅ Import MenubarModule
import { ButtonModule } from 'primeng/button';          // ✅ Import ButtonModule
import { CommonModule } from '@angular/common';         // ✅ Import CommonModule for basic Angular directives

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [DashboardComponent, MenubarModule, ButtonModule, CommonModule], // ✅ Add imports here
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'Crypto Scout';
}
