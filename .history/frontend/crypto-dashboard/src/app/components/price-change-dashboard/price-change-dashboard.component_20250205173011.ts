import { Component, OnInit, ViewChild, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { UIChart } from 'primeng/chart';
import { WebsocketService } from '../../services/websocket.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './price-change-dashboard.component.html',
  styleUrls: ['./price-change-dashboard.component.scss'],
  standalone: true,
  imports: [UIChart]
})
export class PriceDashboardComponent implements OnInit {
  @ViewChild('cryptoChart') cryptoChart!: UIChart;
  chartData: any;
  chartOptions: any;

  // ✅ Track price history for all coins
  priceHistoryMap: { [key: string]: { timestamp: Date; price: number }[] } = {};
  maxDataPoints = 50; // Keep only the latest 50 data points

  isBrowser: boolean;

  constructor(
    private webSocketService: WebsocketService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit(): void {
    this.webSocketService.getData().subscribe((data) => {
      const productId = Object.keys(data)[0];
      const price = data[productId];
      const timestamp = new Date();

      // ✅ Initialize price history for new coins
      if (!this.priceHistoryMap[productId]) {
        this.priceHistoryMap[productId] = [];
      }

      this.priceHistoryMap[productId].push({ timestamp, price });

      // ✅ Limit to the latest 50 data points
      if (this.priceHistoryMap[productId].length > this.maxDataPoints) {
        this.priceHistoryMap[productId].shift();
      }

      this.updateChart();
    });

    this.initializeChartOptions();
  }

  initializeChartOptions(): void {
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
          },
          ticks: {
            autoSkip: true,
            maxTicksLimit: 10
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
    let globalTimeLabels: string[] = [];

    const datasets = Object.keys(this.priceHistoryMap).map((coin, index) => {
      const colors = ['#42A5F5', '#66BB6A', '#FFA726', '#AB47BC', '#FF7043', '#8E44AD', '#E74C3C'];
      const color = colors[index % colors.length];

      const filteredData = this.priceHistoryMap[coin];

      const timeLabels = filteredData.map((item) => item.timestamp.toLocaleTimeString());
      const priceData = filteredData.map((item) => item.price);

      if (timeLabels.length > globalTimeLabels.length) {
        globalTimeLabels = timeLabels;
      }

      return {
        label: coin, // ✅ Display coin name (BTC-USD, ETH-USD, ADA-USD, etc.)
        data: priceData,
        borderColor: color,
        backgroundColor: this.hexToRgba(color, 0.2),
        fill: true
      };
    });

    this.chartData = {
      labels: globalTimeLabels,
      datasets: datasets
    };
  }

  hexToRgba(hex: string, alpha: number): string {
    const bigint = parseInt(hex.replace('#', ''), 16);
    const r = (bigint >> 16) & 255;
    const g = (bigint >> 8) & 255;
    const b = bigint & 255;
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }
}
