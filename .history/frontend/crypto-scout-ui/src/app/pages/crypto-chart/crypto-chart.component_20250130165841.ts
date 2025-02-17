import { Component, OnInit, OnDestroy } from '@angular/core';
import { CryptoWebsocketService, PriceUpdate } from '../../services/crypto-websocket.service';
import { Subscription } from 'rxjs';
import { NgApexchartsModule } from 'ng-apexcharts';
import { CommonModule } from '@angular/common';
import { ApexOptions} from 'apexcharts';

@Component({
  selector: 'app-crypto-chart',
  standalone: true,
  imports: [NgApexchartsModule, CommonModule],
  templateUrl: './crypto-chart.component.html',
  styleUrls: ['./crypto-chart.component.css']
})
export class CryptoChartComponent implements OnInit, OnDestroy {
  public chartOptions: ApexOptions = {
    series: [{ name: 'BTC-USD Price', data: [] }],
    chart: { type: 'line', height: 300 } as ApexChart,  // ✅ Ensure `chart` is strongly typed
    xaxis: { type: 'datetime' },
    yaxis: { title: { text: 'Price (USD)' } },
    title: { text: 'BTC-USD Live Chart', align: 'center' },
    dataLabels: { enabled: false },
    stroke: { curve: 'smooth' } as ApexStroke,  // ✅ Ensure `stroke` is strongly typed
    tooltip: { x: { format: 'HH:mm:ss' } }
  };

  private btcPrices: Array<{ x: string, y: number }> = [];
  private priceSub?: Subscription;

  constructor(private cryptoWsService: CryptoWebsocketService) {}

  ngOnInit(): void {
    this.priceSub = this.cryptoWsService.priceUpdates$.subscribe((update: PriceUpdate) => {
      const btcPrice = update['BTC-USD'];
      if (btcPrice) {
        const time = new Date().toISOString();
        this.btcPrices.push({ x: time, y: btcPrice });

        if (this.btcPrices.length > 50) {
          this.btcPrices.shift();
        }

        this.chartOptions = {
          ...this.chartOptions,
          chart: this.chartOptions.chart ?? { type: 'line', height: 300 },
          stroke: this.chartOptions.stroke ?? { curve: 'smooth' },
          series: [{ name: 'BTC-USD Price', data: [...this.btcPrices] }]
        };
      }
    });
  }

  ngOnDestroy(): void {
    this.priceSub?.unsubscribe();
  }
}
