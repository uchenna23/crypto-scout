import { Component, OnInit } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
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
    RouterLink,
    RouterLinkActive,
    MenubarModule,
    ButtonModule,
    CommonModule,
    PriceChangeDashboardComponent,
    PriceHistoryDashboardComponent
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  title = 'Crypto Scout';
  currentUrl: string = '/';

  constructor(private router: Router) {}

  ngOnInit() {
    this.currentUrl = this.router.url;
    // Subscribe to route changes to update currentUrl.
    this.router.events.subscribe(() => {
      this.currentUrl = this.router.url;
    });
  }

  // Returns true if we're on the home route.
  isHome(): boolean {
    return this.currentUrl === '/' || this.currentUrl === '';
  }
}
