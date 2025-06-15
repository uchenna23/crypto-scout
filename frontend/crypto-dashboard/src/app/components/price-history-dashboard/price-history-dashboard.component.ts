import { Component, OnInit, ViewChild } from '@angular/core';
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
  @ViewChild('priceHistoryChart') priceHistoryChart!: UIChart;

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
          type: 'category' // Using a category axis for date labels
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
      // Build datasets for each coin using filtered data
      const datasets = responses.map((data, index) => {
        const coin = this.coins[index];
        const colors = ['#42A5F5', '#66BB6A', '#FFA726', '#AB47BC', '#FF7043'];
        const color = colors[index % colors.length];

        // Sort the data and then filter by the selected range
        const sortedData = data.sort((a: any, b: any) => a[0] - b[0]);
        const filteredData = this.filterDataByRange(sortedData);

        // Map closing prices for the dataset
        const closingPrices = filteredData.map((entry: any) => entry[4]);

        return {
          label: coin,
          data: closingPrices,
          borderColor: color,
          backgroundColor: this.hexToRgba(color, 0.2),
          fill: true
        };
      });

      // Build the x-axis labels using the filtered data from the first coin
      const firstCoinSortedData = responses[0].sort((a: any, b: any) => a[0] - b[0]);
      const filteredFirstCoinData = this.filterDataByRange(firstCoinSortedData);
      const timeLabels = filteredFirstCoinData.map((entry: any) =>
        new Date(entry[0] * 1000).toLocaleDateString()
      );

      this.chartData = {
        labels: timeLabels,
        datasets: datasets
      };

      // Optionally, if you wish to adjust the zoom further, call zoomToSelectedRange.
      // Note: If the filtering meets your needs, you may not require additional zooming.
      this.zoomToSelectedRange();
    });
  }

  filterDataByRange(data: any[]): any[] {
    const now = new Date();
    const startOf2016 = new Date('2016-01-01').getTime();

    switch (this.selectedRange) {
      case 'mtd': {
        const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1).getTime();
        return data.filter(
          (entry) => entry[0] * 1000 >= startOfMonth && entry[0] * 1000 <= now.getTime()
        );
      }
      case 'ytd': {
        const startOfYear = new Date(now.getFullYear(), 0, 1).getTime();
        return data.filter(
          (entry) => entry[0] * 1000 >= startOfYear && entry[0] * 1000 <= now.getTime()
        );
      }
      default:
        return data.filter((entry) => entry[0] * 1000 >= startOf2016);
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

  zoomToSelectedRange(): void {
    // This function adjusts the visible x-axis range.
    // Since we already filter the data for the labels and datasets,
    // additional zooming may not be strictly necessary.
    if (this.priceHistoryChart && this.priceHistoryChart.chart) {
      const chart = this.priceHistoryChart.chart;
      const labels = chart.data.labels;

      if (this.selectedRange === 'mtd' || this.selectedRange === 'ytd') {
        const now = new Date();
        const startDate =
          this.selectedRange === 'mtd'
            ? new Date(now.getFullYear(), now.getMonth(), 1)
            : new Date(now.getFullYear(), 0, 1);

        const startIndex = labels.findIndex(
          (label: any) => new Date(label).getTime() >= startDate.getTime()
        );
        const endIndex = labels.findIndex(
          (label: any) => new Date(label).getTime() > now.getTime()
        );

        chart.options.scales.x.min = startIndex >= 0 ? startIndex : 0;
        chart.options.scales.x.max = endIndex > 0 ? endIndex : labels.length - 1;
      } else {
        chart.options.scales.x.min = undefined;
        chart.options.scales.x.max = undefined;
      }

      chart.update();
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
