import { Component, OnInit, ViewChild, ElementRef, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { UIChart } from 'primeng/chart';
import { WebsocketService } from '../../services/websocket.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  standalone: true,
  imports: [UIChart]
})
export class DashboardComponent implements OnInit {
  @ViewChild('cryptoChart') cryptoChart!: UIChart;
  chartData: any;
  chartOptions: any;
  priceHistoryMap: { [key: string]: { timestamp: Date, price: number }[] } = {};
  timeLabels: string[] = [];
  currentView: 'DTD' | 'MTD' | 'YTD' = 'DTD';
  isBrowser: boolean;

  constructor(
    private webSocketService: WebsocketService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit(): void {
    this.webSocketService.getData().subscribe(data => {
      const productId = Object.keys(data)[0];
      const price = data[productId];
      const timestamp = new Date();

      if (!this.priceHistoryMap[productId]) {
        this.priceHistoryMap[productId] = [];
      }

      this.priceHistoryMap[productId].push({ timestamp, price });

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

  filterDataByView(view: 'DTD' | 'MTD' | 'YTD') {
    this.currentView = view;
    this.updateChart();
  }

  updateChart(): void {
    const datasets = Object.keys(this.priceHistoryMap).map((coin, index) => {
      const colors = ['#42A5F5', '#66BB6A', '#FFA726', '#AB47BC', '#FF7043'];
      const color = colors[index % colors.length];
      const now = new Date();

      let filteredData = this.priceHistoryMap[coin];

      if (this.currentView === 'DTD') {
        filteredData = filteredData.filter(item => item.timestamp.toDateString() === now.toDateString());
      } else if (this.currentView === 'MTD') {
        filteredData = filteredData.filter(item =>
          item.timestamp.getFullYear() === now.getFullYear() &&
          item.timestamp.getMonth() === now.getMonth()
        );
      } else if (this.currentView === 'YTD') {
        filteredData = filteredData.filter(item =>
          item.timestamp.getFullYear() === now.getFullYear()
        );
      }

      const timeLabels = filteredData.map(item => item.timestamp.toLocaleTimeString());
      const priceData = filteredData.map(item => item.price);

      return {
        label: coin,
        data: priceData,
        borderColor: color,
        backgroundColor: this.hexToRgba(color, 0.2),
        fill: true
      };
    });

    this.chartData = {
      labels: datasets[0]?.data.length ? datasets[0].data.map((_, i) => i + 1) : [],
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
