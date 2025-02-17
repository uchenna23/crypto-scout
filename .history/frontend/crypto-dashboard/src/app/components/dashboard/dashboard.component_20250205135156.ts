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
  priceHistoryMap: { [key: string]: number[] } = {}; // Track price history for each coin
  timeLabels: string[] = [];

  constructor(private webSocketService: WebsocketService) {}

  ngOnInit(): void {
    this.webSocketService.getData().subscribe(data => {
      console.log('📊 WebSocket Data:', data);
      const productId = Object.keys(data)[0]; // Coin name (e.g., BTC-USD)
      const price = data[productId];

      // Initialize price history for new coins
      if (!this.priceHistoryMap[productId]) {
        this.priceHistoryMap[productId] = [];
      }

      // Add price to the corresponding coin's history
      this.priceHistoryMap[productId].push(price);

      // Add time label if it's the first coin or first data point
      if (this.timeLabels.length === 0 || this.priceHistoryMap[productId].length > this.timeLabels.length) {
        this.timeLabels.push(new Date().toLocaleTimeString());
      }

      // Keep only the last 20 data points per coin
      Object.keys(this.priceHistoryMap).forEach(coin => {
        if (this.priceHistoryMap[coin].length > 20) {
          this.priceHistoryMap[coin].shift();
        }
      });
      if (this.timeLabels.length > 20) {
        this.timeLabels.shift();
      }

      this.updateChart();
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

  updateChart(): void {
    // Generate a dataset for each coin
    const datasets = Object.keys(this.priceHistoryMap).map((coin, index) => {
      const colors = ['#42A5F5', '#66BB6A', '#FFA726', '#AB47BC', '#FF7043']; // Add more colors if needed
      const color = colors[index % colors.length];

      return {
        label: coin,
        data: this.priceHistoryMap[coin],
        borderColor: color,
        backgroundColor: this.hexToRgba(color, 0.2),
        fill: true
      };
    });

    // Update the chart data with all coins' datasets
    this.chartData = {
      labels: this.timeLabels,
      datasets: datasets
    };
  }

  // Utility to convert HEX color to RGBA
  hexToRgba(hex: string, alpha: number): string {
    const bigint = parseInt(hex.replace('#', ''), 16);
    const r = (bigint >> 16) & 255;
    const g = (bigint >> 8) & 255;
    const b = bigint & 255;
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }
}
