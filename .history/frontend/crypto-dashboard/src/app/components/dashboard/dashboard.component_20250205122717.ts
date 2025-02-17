import { Component, OnInit } from '@angular/core';
import { ChartModule } from 'primeng/chart';
import { WebsocketService } from '../../services/websocket.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  standalone: true,
  imports: [ChartModule]
})
export class DashboardComponent implements OnInit {
  chartData: any;
  chartOptions: any;
  priceHistory: number[] = [];
  timeLabels: string[] = [];

  constructor(private webSocketService: WebsocketService) {}

  ngOnInit(): void {
    this.webSocketService.getData().subscribe(data => {
      console.log('📊 WebSocket Data:', data);
      const productId = Object.keys(data)[0];
      const price = data[productId];

      // Add data to arrays
      this.priceHistory.push(price);
      this.timeLabels.push(new Date().toLocaleTimeString());

      // Keep only the last 20 data points
      if (this.priceHistory.length > 20) {
        this.priceHistory.shift();
        this.timeLabels.shift();
      }

      this.updateChart(productId);
    });

    this.chartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: true,
          position: 'top'
        }
      },
      scales: {
        x: {
          display: true,
          title: {
            display: true,
            text: 'Time'
          }
        },
        y: {
          display: true,
          title: {
            display: true,
            text: 'Price (USD)'
          }
        }
      }
    };
  }

  updateChart(productId: string): void {
    this.chartData = {
      labels: this.timeLabels,
      datasets: [
        {
          label: productId,
          data: this.priceHistory,
          borderColor: '#42A5F5',
          backgroundColor: 'rgba(66, 165, 245, 0.2)',
          fill: true
        }
      ]
    };
  }
}
