import { Component, OnInit, ViewChild, ElementRef, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { UIChart } from 'primeng/chart';
import { WebsocketService } from '../../services/websocket.service';
import zoomPlugin from 'chartjs-plugin-zoom';
import { Chart } from 'chart.js';

Chart.register(zoomPlugin);

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
  priceHistoryMap: { [key: string]: number[] } = {};
  timeLabels: string[] = [];
  isBrowser: boolean;

  constructor(
    private webSocketService: WebsocketService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  async ngOnInit(): Promise<void> {
    if (this.isBrowser) {
      const hammer = await import('hammerjs');
      console.log('HammerJS loaded:', hammer);
    }

    this.webSocketService.getData().subscribe(data => {
      const productId = Object.keys(data)[0];
      const price = data[productId];

      if (!this.priceHistoryMap[productId]) {
        this.priceHistoryMap[productId] = [];
      }

      this.priceHistoryMap[productId].push(price);

      if (this.timeLabels.length === 0 || this.priceHistoryMap[productId].length > this.timeLabels.length) {
        this.timeLabels.push(new Date().toLocaleTimeString());
      }

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
        },
        zoom: {
          pan: {
            enabled: true,
            mode: 'xy',
            threshold: 5 // ✅ Prevent accidental panning
          },
          zoom: {
            wheel: {
              enabled: true,
              speed: 0.1 // ✅ Smoother zooming
            },
            pinch: {
              enabled: true
            },
            mode: 'xy',
            limits: {
              x: { min: 0, max: 20 },     // ✅ Limit x-axis zoom
              y: { min: 0, max: 10000 }   // ✅ Limit y-axis zoom based on expected price ranges
            }
          }
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
          },
          ticks: {
            stepSize: 5,              // ✅ Smaller steps for better price visibility
            autoSkip: false            // Show all ticks without skipping
          },
          beginAtZero: false,          // ✅ Start from the minimum value instead of zero
          min: 0                       // Optional: Adjust this based on your price range
        }
      }
    };
  }
  

  updateChart(): void {
    const datasets = Object.keys(this.priceHistoryMap).map((coin, index) => {
      const colors = ['#42A5F5', '#66BB6A', '#FFA726', '#AB47BC', '#FF7043'];
      const color = colors[index % colors.length];

      return {
        label: coin,
        data: this.priceHistoryMap[coin],
        borderColor: color,
        backgroundColor: this.hexToRgba(color, 0.2),
        fill: true
      };
    });

    this.chartData = {
      labels: this.timeLabels,
      datasets: datasets
    };
  }

  resetZoom(): void {
    if (this.cryptoChart && this.cryptoChart.chart) {
      this.cryptoChart.chart.resetZoom();
    }
  }

  hexToRgba(hex: string, alpha: number): string {
    const bigint = parseInt(hex.replace('#', ''), 16);
    const r = (bigint >> 16) & 255;
    const g = (bigint >> 8) & 255;
    const b = bigint & 255;
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }
}
