import { Component, OnInit, OnDestroy } from '@angular/core';
import { CryptoWebsocketService, PriceUpdate } from '../../services/crypto-websocket.service';
import { Subscription } from 'rxjs';
import { NgApexchartsModule, ChartType } from 'ng-apexcharts'; // ✅ Note ChartType from ng-apexcharts
import { CommonModule } from '@angular/common';
import { ApexOptions } from 'apexcharts';

@Component({
  selector: 'app-crypto-chart',
  standalone: true,
  imports: [NgApexchartsModule, CommonModule],
  templateUrl: './crypto-chart.component.html',
  styleUrls: ['./crypto-chart.component.css']
})
export class CryptoChartComponent implements OnInit, OnDestroy {
  public chartOptions: ApexOptions = {
    series: [
      {
        name: 'BTC-USD Price',
        data: [] // ✅ Always an array, never undefined
      }
    ],
    chart: {
      type: 'line' as ChartType, // ✅ Force recognized type, e.g. 'line', 'area', 'bar', etc.
      height: 300
    },
    xaxis: {
      type: 'datetime'
    },
    yaxis: {
      title: { text: 'Price (USD)' }
    },
    title: {
      text: 'BTC-USD Live Chart',
      align: 'center'
    },
    dataLabels: {
      enabled: false
    },
    stroke: {
      // The recognized options are 'smooth', 'straight', 'stepline', 'monotoneCubic'
      curve: 'smooth' as 'smooth'
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
        const time = new Date().toISOString();
        this.btcPrices.push({ x: time, y: btcPrice });

        // Keep last 50 points
        if (this.btcPrices.length > 50) {
          this.btcPrices.shift();
        }

        // Update chart series with the new data
        this.chartOptions = {
          ...this.chartOptions,
          series: [
            {
              name: 'BTC-USD Price',
              data: [...this.btcPrices]
            }
          ]
        };
      }
    });
  }

  ngOnDestroy(): void {
    this.priceSub?.unsubscribe();
  }
}
