import { Component, OnInit } from '@angular/core';
import { ChartModule } from 'primeng/chart'; // ✅ Import ChartModule
import { WebsocketService } from '../../services/websocket.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  standalone: true,
  imports: [ChartModule] // ✅ Ensure ChartModule is imported
})
export class DashboardComponent implements OnInit {
  chartData: any;
  chartOptions: any;
  cryptoData: any = {};

  constructor(private webSocketService: WebsocketService){}

  ngOnInit(): void {
    this.webSocketService.getData().subscribe(data =>{
      console.log('📊 WebSocket Data:', data);
      this.cryptoData.data;
    })
    
    this.chartData = {
      labels: ['January', 'February', 'March', 'April'],
      datasets: [
        {
          label: 'BTC-USD',
          data: [40000, 42000, 41000, 43000],
          borderColor: '#42A5F5',
          fill: false
        }
      ]
    };

    this.chartOptions = {
      responsive: true,
      maintainAspectRatio: false
    };
  }
}
