import { Component, OnInit, OnDestroy } from '@angular/core';
import { CryptoWebsocketService, PriceUpdate } from '../../services/crypto-websocket.service';
import { Subscription } from 'rxjs';
import {
  ChartComponent,
  ApexChart,
  ApexXAxis,
  ApexYAxis,
  ApexTitleSubtitle,
  ApexStroke,
  ApexDataLabels,
  ApexTooltip,
  ApexAxisChartSeries
} from 'ng-apexcharts';
import { NgApexchartsModule } from 'ng-apexcharts'; // ✅ Import ApexCharts
import { CommonModule } from '@angular/common'; // ✅ Required for standalone components

export type ChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis;
  title: ApexTitleSubtitle;
  dataLabels: ApexDataLabels;
  stroke: ApexStroke;
  tooltip: ApexTooltip;
};

@Component({
  selector: 'app-crypto-chart',
  standalone: true, // ✅ Required for Angular 19
  imports: [NgApexchartsModule, CommonModule], // ✅ Ensure required imports
  templateUrl: './crypto-chart.component.html',
  styleUrls: ['./crypto-chart.component.css']
})
export class CryptoChartComponent implements OnInit, OnDestroy {
  public chartOptions: Partial<ChartOptions>;
  private btcPrices: Array<{ x: string, y: number }> = [];
  private priceSub?: Subscription;

  constructor(private cryptoWsService: CryptoWebsocketService) {
    this.chartOptions = {
      series: [{ name: 'BTC-USD Price', data: [] }],
      chart: { type: 'line', height: 300 },
      xaxis: { type: 'datetime' },
      title: { text: 'BTC-USD Live Chart', align: 'center' },
      dataLabels: { enabled: false },
      stroke: { curve: 'smooth' },
      tooltip: { x: { format: 'HH:mm:ss' } }
    };
  }

  ngOnInit(): void {
    this.priceSub = this.cryptoWsService.priceUpdates$.subscribe((update: PriceUpdate) => {
      const btcPrice = update['BTC-USD'];
      if (btcPrice) {
        const time = new Date().toISOString();
        this.btcPrices.push({ x: time, y: btcPrice });

        if (this.btcPrices.length > 50) {
          this.btcPrices.shift();
        }

        this.chartOptions.series = [{ name: 'BTC-USD Price', data: [...this.btcPrices] }];
      }
    });
  }

  ngOnDestroy(): void {
    this.priceSub?.unsubscribe();
  }
}
