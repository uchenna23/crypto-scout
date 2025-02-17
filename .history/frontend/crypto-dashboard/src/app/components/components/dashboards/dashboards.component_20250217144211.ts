import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PriceChangeDashboardComponent } from '../../price-change-dashboard/price-change-dashboard.component';
import { PriceHistoryDashboardComponent } from '../../price-history-dashboard/price-history-dashboard.component';

@Component({
  selector: 'app-dashboards',
  standalone: true,
  imports: [CommonModule, PriceChangeDashboardComponent, PriceHistoryDashboardComponent],
  templateUrl: './dashboards.component.html',
  styleUrls: ['./dashboards.component.scss']
})
export class DashboardsComponent {}
