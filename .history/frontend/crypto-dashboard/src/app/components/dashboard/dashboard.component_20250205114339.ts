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
  cryptoPrices: { [key: string]: number[] } = {}; // Store price history
  labels: string[] = [];

  constructor(private webSocketService: WebsocketService) {}

  ngOnInit(): void {
    this.initializeChart();

    this.webSocketService.getData().subscribe(data => {
      console.log('📊 WebSocket Data:', data);
      this.updateChart(data);
    });
  }

  initializeChart() {
    this.chartData = {
      labels: [],
      datasets: [
        {
          label: 'BTC-USD',
          data: [],
          borderColor: '#42A5F5',
          fill: false
        },
        {
          label: 'ETH-USD',
          data: [],
          borderColor: '#FFA726',
          fill: false
        }
      ]
    };

    this.chartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: { display: true },
        y: { display: true }
      }
    };
  }

  updateChart(newData: any) {
    const timestamp = new Date().toLocaleTimeString();

    Object.keys(newData).forEach((crypto) => {
      if (!this.cryptoPrices[crypto]) {
        this.cryptoPrices[crypto] = [];
      }

      this.cryptoPrices[crypto].push(newData[crypto]);

      // Keep only last 10 data points for each crypto
      if (this.cryptoPrices[crypto].length > 10) {
        this.cryptoPrices[crypto].shift();
      }
    });

    // Keep track of timestamps for X-axis
    this.labels.push(timestamp);
    if (this.labels.length > 10) {
      this.labels.shift();
    }

    // Update chartData dynamically
    this.chartData = {
      labels: [...this.labels],
      datasets: [
        {
          label: 'BTC-USD',
          data: this.cryptoPrices['BTC-USD'] || [],
          borderColor: '#42A5F5',
          fill: false
        },
        {
          label: 'ETH-USD',
          data: this.cryptoPrices['ETH-USD'] || [],
          borderColor: '#FFA726',
          fill: false
        }
      ]
    };
  }
}
