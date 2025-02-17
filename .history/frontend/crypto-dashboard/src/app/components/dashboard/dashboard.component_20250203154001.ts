import { Component, OnInit } from '@angular/core';
import { ChartModule } from 'primeng/chart'; // ✅ Import ChartModule

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  standalone: true,
  imports: [ChartModule] // ✅ Ensure ChartModule is imported
})
export class DashboardComponent implements OnInit {
  chartData: any;
  chartOptions: any;

  ngOnInit(): void {
    this.chartData = {
      labels: ['January', 'February', 'March', 'April'],
      datasets: [
        {
          label: 'BTC-USD',
          data: [40000, 42000, 41000, 43000],
          borderColor: '#42A5F5',
          fill: false
        }
      ]
    };

    this.chartOptions = {
      responsive: true,
      maintainAspectRatio: false
    };
  }
}
