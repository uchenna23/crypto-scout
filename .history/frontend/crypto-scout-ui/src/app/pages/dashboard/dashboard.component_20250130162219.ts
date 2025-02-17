import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { CryptoWebsocketService, PriceUpdate } from '../../services/crypto-websocket.service';
import { CryptoChartComponent } from '../crypto-chart/crypto-chart.component'; // ✅ Import chart component
import { CommonModule } from '@angular/common'; // ✅ Required for *ngFor

@Component({
  selector: 'app-crypto-dashboard',
  standalone: true, // ✅ Make it a standalone component
  imports: [CommonModule, CryptoChartComponent], // ✅ Add CryptoChartComponent
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class CryptoDashboardComponent implements OnInit, OnDestroy {
  public prices: { [productId: string]: number } = {};
  private priceSub?: Subscription;

  constructor(private cryptoWsService: CryptoWebsocketService) {}

  ngOnInit(): void {
    this.priceSub = this.cryptoWsService.priceUpdates$.subscribe((update: PriceUpdate) => {
      Object.keys(update).forEach(productId => {
        this.prices[productId] = update[productId];
      });
    });
  }

  ngOnDestroy(): void {
    this.priceSub?.unsubscribe();
  }

  getProductIds(): string[] {
    return Object.keys(this.prices);
  }
}
