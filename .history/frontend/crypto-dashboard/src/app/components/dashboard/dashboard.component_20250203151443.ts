import { Component, OnInit } from '@angular/core';
import { WebsocketService } from '../../services/websocket.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  chartData: any;
  chartOptions: any;

  constructor(private websocketService: WebsocketService) {}

  ngOnInit(): void {
    this.initializeChart();

    this.websocketService.getData().subscribe((data) => {
      this.updateChart(data);
    });
  }

  private initializeChart() {
    this.chartData = {
      labels: [],
      datasets: [
        {
          label: 'BTC-USD',
          data: [],
          borderColor: '#42A5F5',
          fill: false,
        },
        {
          label: 'ETH-USD',
          data: [],
          borderColor: '#66BB6A',
          fill: false,
        }
      ]
    };

    this.chartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: {
          title: {
            display: true,
            text: 'Time'
          }
        },
        y: {
          title: {
            display: true,
            text: 'Price (USD)'
          }
        }
      }
    };
  }

  private updateChart(data: any) {
    const timestamp = new Date().toLocaleTimeString();

    Object.keys(data).forEach((key) => {
      const dataset = this.chartData.datasets.find((d: { label: string; }) => d.label === key);
      if (dataset) {
        dataset.data.push(data[key]);
        if (dataset.data.length > 20) {
          dataset.data.shift();
        }
      }
    });

    this.chartData.labels.push(timestamp);
    if (this.chartData.labels.length > 20) {
      this.chartData.labels.shift();
    }
  }
}
