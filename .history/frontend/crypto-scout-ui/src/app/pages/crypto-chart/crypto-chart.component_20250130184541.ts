import { Component, OnInit, OnDestroy } from '@angular/core';
import { CryptoWebsocketService, PriceUpdate } from '../../services/crypto-websocket.service';
import { Subscription } from 'rxjs';
import { ApexOptions } from 'ng-apexcharts'; // ✅ Fix import

@Component({
  selector: 'app-crypto-chart',
  standalone: true,
  imports: [],
  templateUrl: './crypto-chart.component.html',
  styleUrls: ['./crypto-chart.component.css']
})
export class CryptoChartComponent implements OnInit, OnDestroy {
  public chartOptions!: ApexOptions; // ✅ Ensure it's initialized properly

  private btcPrices: Array<{ x: string, y: number }> = [];
  private priceSub?: Subscription;

  constructor(private cryptoWsService: CryptoWebsocketService) {}

  ngOnInit(): void {
    this.initializeChart();

    this.priceSub = this.cryptoWsService.priceUpdates$.subscribe((update: PriceUpdate) => {
      const btcPrice = update['BTC-USD'];
      if (btcPrice) {
        const time = new Date().toISOString();
        this.btcPrices.push({ x: time, y: btcPrice });

        if (this.btcPrices.length > 50) {
          this.btcPrices.shift();
        }

        // ✅ Properly update chartOptions
        this.chartOptions = {
          ...this.chartOptions,
          series: [{ name: 'BTC-USD Price', data: [...this.btcPrices] }]
        };
      }
    });
  }

  ngOnDestroy(): void {
    this.priceSub?.unsubscribe();
  }

  private initializeChart(): void {
    this.chartOptions = {
      series: [{ name: 'BTC-USD Price', data: [] }],
      chart: { type: 'line', height: 300 },
      xaxis: { type: 'datetime' },
      yaxis: { title: { text: 'Price (USD)' } },
      title: { text: 'BTC-USD Live Chart', align: 'center' },
      dataLabels: { enabled: false },
      stroke: { curve: 'smooth' },
      tooltip: { x: { format: 'HH:mm:ss' } }
    };
  }
}
