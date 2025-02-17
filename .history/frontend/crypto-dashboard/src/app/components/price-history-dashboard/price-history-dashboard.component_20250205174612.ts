import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { UIChart } from 'primeng/chart';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-price-history-dashboard',
  templateUrl: './price-history-dashboard.component.html',
  styleUrls: ['./price-history-dashboard.component.scss'],
  standalone: true,
  imports: [UIChart, HttpClientModule]
})
export class PriceHistoryDashboardComponent implements OnInit {
  chartData: any;
  chartOptions: any;

  // ✅ Supported coins
  coins = ['BTC-USD', 'ETH-USD', 'ADA-USD', 'SOL-USD', 'XRP-USD'];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.initializeChartOptions();
    this.fetchPriceHistory();
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
            text: 'Date'
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

  fetchPriceHistory(): void {
    const requests = this.coins.map((coin) =>
      this.http.get<any>(`https://api.exchange.coinbase.com/products/${coin}/candles?granularity=86400`)
    );

    forkJoin(requests).subscribe((responses) => {
      const datasets = responses.map((data, index) => {
        const coin = this.coins[index];

        const colors = ['#42A5F5', '#66BB6A', '#FFA726', '#AB47BC', '#FF7043'];
        const color = colors[index % colors.length];

        // ✅ Coinbase OHLC format: [timestamp, low, high, open, close, volume]
        const sortedData = data.sort((a: any, b: any) => a[0] - b[0]);

        const labels = sortedData.map((entry: any) =>
          new Date(entry[0] * 1000).toLocaleDateString()
        );

        const closingPrices = sortedData.map((entry: any) => entry[4]); // Closing price

        return {
          label: coin,
          data: closingPrices,
          borderColor: color,
          backgroundColor: this.hexToRgba(color, 0.2),
          fill: true
        };
      });

      const timeLabels = responses[0]
        .sort((a: any, b: any) => a[0] - b[0])
        .map((entry: any) => new Date(entry[0] * 1000).toLocaleDateString());

      this.chartData = {
        labels: timeLabels,
        datasets: datasets
      };
    });
  }

  hexToRgba(hex: string, alpha: number): string {
    const bigint = parseInt(hex.replace('#', ''), 16);
    const r = (bigint >> 16) & 255;
    const g = (bigint >> 8) & 255;
    const b = bigint & 255;
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }
}
