import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { CryptoWebsocketService, PriceUpdate } from '../../services/crypto-websocket.service';

@Component({
  selector: 'app-crypto-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class CryptoDashboardComponent implements OnInit, OnDestroy {
  // Stores the latest price for each product
  public prices: { [productId: string]: number } = {};
  
  private priceSub?: Subscription;

  constructor(private cryptoWsService: CryptoWebsocketService) {}

  ngOnInit(): void {
    // Subscribe to price updates
    this.priceSub = this.cryptoWsService.priceUpdates$.subscribe((update: PriceUpdate) => {
      // For example: update might be { "BTC-USD": 12345.67 }
      // Merge into the 'prices' object
      Object.keys(update).forEach(productId => {
        this.prices[productId] = update[productId];
      });
    });
  }

  ngOnDestroy(): void {
    // Unsubscribe to avoid memory leaks
    this.priceSub?.unsubscribe();
  }

  getProductIds(): string[] {
    return Object.keys(this.prices);
  }
}
