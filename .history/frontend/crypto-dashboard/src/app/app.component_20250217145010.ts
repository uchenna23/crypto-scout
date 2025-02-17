import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MenubarModule } from 'primeng/menubar';
import { ButtonModule } from 'primeng/button'; 
import { CommonModule } from '@angular/common';
import { PriceChangeDashboardComponent } from "./components/price-change-dashboard/price-change-dashboard.component";
import { PriceHistoryDashboardComponent } from "./components/price-history-dashboard/price-history-dashboard.component";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    MenubarModule,
    ButtonModule,
    CommonModule,
    PriceChangeDashboardComponent,
    PriceHistoryDashboardComponent
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'Crypto Scout';
}
