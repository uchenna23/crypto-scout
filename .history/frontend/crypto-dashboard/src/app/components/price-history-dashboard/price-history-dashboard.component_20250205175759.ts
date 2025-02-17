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

  coins = ['BTC-USD', 'ETH-USD', 'ADA-USD', 'SOL-USD', 'XRP-USD'];
  selectedRange = 'default'; // Default to show price history from 2016

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
            maxTicksLimit: 10,
            callback: function(value: any) {
              const date = new Date(value);
              return `${date.getDate()}-${date.toLocaleString('default', { month: 'short' })}`;
            }
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

        const sortedData = data.sort((a: any, b: any) => a[0] - b[0]);

        const filteredData = this.filterDataByRange(sortedData);

        const labels = filteredData.map((entry: any) =>
          new Date(entry[0] * 1000).toLocaleDateString()
        );

        const closingPrices = filteredData.map((entry: any) => entry[4]);

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

  filterDataByRange(data: any[]): any[] {
    const now = new Date();
    const startOf2025 = new Date('2025-01-01').getTime();
    const startOf2016 = new Date('2016-01-01').getTime();

    switch (this.selectedRange) {
      case 'mtd':
        return data.filter((entry) => {
          const date = new Date(entry[0] * 1000);
          return date.getTime() >= startOf2025 && date.getMonth() === now.getMonth() && date.getFullYear() === now.getFullYear();
        });
      case 'ytd':
        return data.filter((entry) => {
          const date = new Date(entry[0] * 1000);
          return date.getTime() >= startOf2025 && date.getFullYear() === now.getFullYear();
        });
      default:
        return data.filter((entry) => new Date(entry[0] * 1000).getTime() >= startOf2016);
    }
  }

  onRangeChange(range: string): void {
    this.selectedRange = range;
    this.fetchPriceHistory();
  }

  resetToDefault(): void {
    this.selectedRange = 'default';
    this.fetchPriceHistory();
  }

  hexToRgba(hex: string, alpha: number): string {
    const bigint = parseInt(hex.replace('#', ''), 16);
    const r = (bigint >> 16) & 255;
    const g = (bigint >> 8) & 255;
    const b = bigint & 255;
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }
}
