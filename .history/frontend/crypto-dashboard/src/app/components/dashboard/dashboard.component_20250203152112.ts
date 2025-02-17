import { Component, OnInit } from '@angular/core';
import { ChartModule } from 'primeng/chart'; // ✅ Import ChartModule

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  standalone: true,  // ✅ Standalone Component
  imports: [ChartModule] // ✅ Import ChartModule here
})
export class DashboardComponent implements OnInit {
  chartData: any;
  chartOptions: any;

  constructor() {}

  ngOnInit(): void {
    this.initializeChart();
  }

  private initializeChart() {
    this.chartData = {
      labels: [],
      datasets: [
        {
          label: 'BTC-USD',
          data: [],
          borderColor: '#42A5F5',
          fill: false,
        },
        {
          label: 'ETH-USD',
          data: [],
          borderColor: '#66BB6A',
          fill: false,
        }
      ]
    };

    this.chartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: {
          title: { display: true, text: 'Time' }
        },
        y: {
          title: { display: true, text: 'Price (USD)' }
        }
      }
    };
  }
}
