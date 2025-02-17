import { Component, OnInit, OnDestroy } from '@angular/core';
import { CryptoWebsocketService, PriceUpdate } from '../../services/crypto-websocket.service';
import { Subscription } from 'rxjs';

import {
  ApexChart,
  ApexXAxis,
  ApexYAxis,
  ApexTitleSubtitle,
  ApexStroke,
  ApexDataLabels,
  ApexTooltip,
  ApexAxisChartSeries
} from 'ng-apexcharts';

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
  templateUrl: './crypto-chart.component.html',
  styleUrls: ['./crypto-chart.component.css']
})
export class CryptoChartComponent implements OnInit, OnDestroy {
  // Chart config
  public chartOptions: Partial<ChartOptions>;

  // We'll track BTC-USD over time for this example
  private btcPrices: Array<{ x: string, y: number }> = [];
  private priceSub?: Subscription;

  constructor(private cryptoWsService: CryptoWebsocketService) {
    // Initialize the chart
    this.chartOptions = {
      series: [
        {
          name: 'BTC-USD Price',
          data: [] // We'll fill this in at runtime
        }
      ],
      chart: {
        type: 'line',
        height: 300
      },
      xaxis: {
        type: 'datetime'
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
  }

  ngOnInit(): void {
    this.priceSub = this.cryptoWsService.priceUpdates$.subscribe((update: PriceUpdate) => {
      // For example: update might be { "BTC-USD": 105293.56 }
      // If it includes BTC-USD, we push it into our chart data
      const btcPrice = update['BTC-USD'];
      if (btcPrice) {
        const time = new Date().toISOString(); // or new Date().getTime()
        this.btcPrices.push({ x: time, y: btcPrice });

        // Trim the array to last 50 points if you want
        if (this.btcPrices.length > 50) {
          this.btcPrices.shift(); 
        }

        // Update chart data
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
