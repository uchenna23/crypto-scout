import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ChartModule } from 'primeng/chart';
import { WebsocketService } from '../../services/websocket.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  standalone: true,
  imports: [ChartModule] 
})
export class DashboardComponent implements OnInit {
  chartData: any;
  chartOptions: any;
  cryptoPrices: { [key: string]: number[] } = {};
  labels: string[] = [];

  constructor(private webSocketService: WebsocketService, private cdRef: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.initializeChart();

    this.webSocketService.getData().subscribe(data => {
      console.log('📊 WebSocket Data:', data);
      this.updateChart(data);
    });
  }

  initializeChart() {
    this.chartData = {
      labels: [],
      datasets: []
    };

    this.chartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: { display: true },
        y: { display: true }
      }
    };
  }

  updateChart(newData: any) {
    const timestamp = new Date().toLocaleTimeString();

    // Ensure we have a dataset for each cryptocurrency
    Object.keys(newData).forEach(crypto => {
      if (!this.cryptoPrices[crypto]) {
        this.cryptoPrices[crypto] = [];
      }

      this.cryptoPrices[crypto].push(newData[crypto]);

      // Keep last 10 data points
      if (this.cryptoPrices[crypto].length > 10) {
        this.cryptoPrices[crypto].shift();
      }
    });

    // Maintain a moving window of 10 timestamps
    this.labels.push(timestamp);
    if (this.labels.length > 10) {
      this.labels.shift();
    }

    // 🔹 **Ensure datasets are properly mapped**
    this.chartData = {
      labels: [...this.labels],
      datasets: Object.keys(this.cryptoPrices).map((crypto, index) => ({
        label: crypto,
        data: this.cryptoPrices[crypto] || [],
        borderColor: this.getColor(index),
        fill: false
      }))
    };

    // 🔹 Force Angular to detect changes
    this.cdRef.detectChanges();
  }

  // Helper to assign different colors to each crypto
  private getColor(index: number): string {
    const colors = ['#42A5F5', '#FFA726', '#66BB6A', '#FF7043', '#AB47BC'];
    return colors[index % colors.length];
  }
}
