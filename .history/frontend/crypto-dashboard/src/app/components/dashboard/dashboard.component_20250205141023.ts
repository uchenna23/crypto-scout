import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { ChartModule } from 'primeng/chart';
import { WebsocketService } from '../../services/websocket.service';
import zoomPlugin from 'chartjs-plugin-zoom';
import { Chart } from 'chart.js';

// Register the zoom plugin
Chart.register(zoomPlugin);

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  standalone: true,
  imports: [ChartModule]
})
export class DashboardComponent implements OnInit {
  @ViewChild('cryptoChart') cryptoChart!: ElementRef; // Access the chart element

  chartData: any;
  chartOptions: any;
  priceHistoryMap: { [key: string]: number[] } = {};
  timeLabels: string[] = [];

  constructor(private webSocketService: WebsocketService) {}

  ngOnInit(): void {
    this.webSocketService.getData().subscribe(data => {
      console.log('📊 WebSocket Data:', data);
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
            mode: 'xy'
          },
          zoom: {
            wheel: {
              enabled: true
            },
            pinch: {
              enabled: true
            },
            mode: 'xy'
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
          }
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

  // ✅ Reset Zoom Function
  resetZoom(): void {
    if (this.cryptoChart) {
      const chartInstance = (this.cryptoChart.nativeElement as any).chart; // Access the Chart.js instance
      if (chartInstance && chartInstance.resetZoom) {
        chartInstance.resetZoom();
      }
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
