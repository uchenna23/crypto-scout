import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { UIChart } from 'primeng/chart';

@Component({
  selector: 'app-price-history-dashboard',
  templateUrl: './price-history-dashboard.component.html',
  styleUrls: ['./price-history-dashboard.component.scss'],
  standalone: true,
  imports: [UIChart]
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
    const historicalDataRequests = this.coins.map((coin) => 
      this.http.get<any>(`https://api.coinbase.com/v2/prices/${coin}/historic?granularity=86400`) // ✅ Daily price history
    );

    Promise.all(historicalDataRequests).then((responses) => {
      const datasets = responses.map((response, index) => {
        const data = response.data.prices;
        const coin = this.coins[index];

        const colors = ['#42A5F5', '#66BB6A', '#FFA726', '#AB47BC', '#FF7043'];
        const color = colors[index % colors.length];

        const labels = data.map((entry: any) => new Date(entry.time * 1000).toLocaleDateString());
        const prices = data.map((entry: any) => parseFloat(entry.price));

        return {
          label: coin,
          data: prices,
          borderColor: color,
          backgroundColor: this.hexToRgba(color, 0.2),
          fill: true
        };
      });

      const timeLabels = responses[0].data.prices.map((entry: any) => 
        new Date(entry.time * 1000).toLocaleDateString()
      );

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
