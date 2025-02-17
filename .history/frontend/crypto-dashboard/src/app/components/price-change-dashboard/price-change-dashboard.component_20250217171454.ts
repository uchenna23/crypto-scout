import { Component, OnInit, ViewChild, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { UIChart } from 'primeng/chart';
import { WebsocketService } from '../../services/websocket.service';
import { PriceChangeService } from '../../services/price-change.service';

@Component({
  selector: 'app-price-change-dashboard',
  templateUrl: './price-change-dashboard.component.html',
  styleUrls: ['./price-change-dashboard.component.scss'],
  standalone: true,
  imports: [UIChart]
})
export class PriceChangeDashboardComponent implements OnInit {
  @ViewChild('cryptoChart') cryptoChart!: UIChart;
  chartData: any;
  chartOptions: any;
  
  isBrowser: boolean;

  constructor(
    private webSocketService: WebsocketService,
    private priceChangeService: PriceChangeService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit(): void {
    // Initialize chart options and update chart with any existing data
    this.initializeChartOptions();
    this.updateChart();

    // Subscribe to WebSocket data
    this.webSocketService.getData().subscribe((data) => {
      const productId = Object.keys(data)[0];
      const price = data[productId];
      
      // Update the shared service state
      this.priceChangeService.updatePrice(productId, price);

      // Update the chart using the persisted data
      this.updateChart();
    });
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

    // Use the price history from the shared service
    const priceHistoryMap = this.priceChangeService.priceHistoryMap;
    
    const datasets = Object.keys(priceHistoryMap).map((coin, index) => {
      const colors = ['#42A5F5', '#66BB6A', '#FFA726', '#AB47BC', '#FF7043', '#8E44AD', '#E74C3C'];
      const color = colors[index % colors.length];

      const filteredData = priceHistoryMap[coin];

      const timeLabels = filteredData.map((item) => item.timestamp.toLocaleTimeString());
      const priceData = filteredData.map((item) => item.price);

      if (timeLabels.length > globalTimeLabels.length) {
        globalTimeLabels = timeLabels;
      }

      return {
        label: coin,
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
