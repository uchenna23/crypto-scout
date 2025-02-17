import { Injectable } from '@angular/core';

export interface PriceData {
  timestamp: Date;
  price: number;
}

@Injectable({
  providedIn: 'root'
})
export class PriceHistoryService {
  public priceHistoryMap: { [key: string]: PriceData[] } = {};
  private maxDataPoints = 50;

  updatePrice(productId: string, price: number): void {
    const timestamp = new Date();
    if (!this.priceHistoryMap[productId]) {
      this.priceHistoryMap[productId] = [];
    }
    this.priceHistoryMap[productId].push({ timestamp, price });
    if (this.priceHistoryMap[productId].length > this.maxDataPoints) {
      this.priceHistoryMap[productId].shift();
    }
  }
}
