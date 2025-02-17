import { Component, OnInit, OnDestroy } from '@angular/core';
import { CryptoWebsocketService, PriceUpdate } from '../../services/crypto-websocket.service';
import { Subscription } from 'rxjs';
import { NgApexchartsModule } from 'ng-apexcharts';
import { CommonModule } from '@angular/common';

// ✅ IMPORTANT: Import all types from 'ng-apexcharts' (not apexcharts!)
import {
  ChartComponent,
  ChartType,
  ApexAxisChartSeries,
  ApexNonAxisChartSeries,
  ApexChart,
  ApexXAxis,
  ApexYAxis,
  ApexTitleSubtitle,
  ApexDataLabels,
  ApexStroke,
  ApexTooltip
} from 'ng-apexcharts';

// ✅ Define a local type that includes everything we need, with NO undefined
type ChartOptions = {
  series: ApexAxisChartSeries | ApexNonAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  yaxis: ApexYAxis | ApexYAxis[];
  title: ApexTitleSubtitle;
  dataLabels: ApexDataLabels;
  stroke: ApexStroke;
  tooltip: ApexTooltip;
};

@Component({
  selector: 'app-crypto-chart',
  standalone: true,
  imports: [NgApexchartsModule, CommonModule],
  templateUrl: './crypto-chart.component.html',
  styleUrls: ['./crypto-chart.component.css']
})
export class CryptoChartComponent implements OnInit, OnDestroy {
  // 🔥 Use our ChartOptions, with NO undefined properties
  public chartOptions: ChartOptions = {
    series: [
      {
        name: 'BTC-USD Price',
        data: []
      }
    ],
    chart: {
      type: 'line' as ChartType, // Ex: "line", "area", "bar", ...
      height: 300
    },
    xaxis: {
      type: 'datetime'
    },
    yaxis: {
      title: {
        text: 'Price (USD)'
      }
    },
    title: {
      text: 'BTC-USD Live Chart',
      align: 'center'
    },
    dataLabels: {
      enabled: false
    },
    stroke: {
      curve: 'smooth'
    },
    tooltip: {
      x: {
        format: 'HH:mm:ss'
      }
    }
  };

  private btcPrices: Array<{ x: string; y: number }> = [];
  private priceSub?: Subscription;

  constructor(private cryptoWsService: CryptoWebsocketService) {}

  ngOnInit(): void {
    // Subscribe to real-time price updates
    this.priceSub = this.cryptoWsService.priceUpdates$.subscribe((update: PriceUpdate) => {
      const btcPrice = update['BTC-USD'];
      if (btcPrice) {
        const now = new Date().toISOString();
        this.btcPrices.push({ x: now, y: btcPrice });

        // Keep last 50 points
        if (this.btcPrices.length > 50) {
          this.btcPrices.shift();
        }

        // ⚡ Update series with fresh data
        this.chartOptions.series = [
          {
            name: 'BTC-USD Price',
            data: [...this.btcPrices]
          }
        ];
      }
    });
  }

  ngOnDestroy(): void {
    this.priceSub?.unsubscribe();
  }
}
