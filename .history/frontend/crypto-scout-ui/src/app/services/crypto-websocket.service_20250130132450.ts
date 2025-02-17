import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject, timer } from 'rxjs';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { catchError, retryWhen, switchMap } from 'rxjs/operators';

/**
 * Shape of a price update message, e.g. { "BTC-USD": 12345.67 }
 */
export interface PriceUpdate {
  [productId: string]: number;
}

@Injectable({
  providedIn: 'root'
})
export class CryptoWebsocketService implements OnDestroy {

  private WS_ENDPOINT = 'ws://localhost:8080/ws'; // ✅ Spring Boot WebSocket URL
  private reconnectInterval = 5000; // 5 seconds
  private coinbaseSocket$?: WebSocketSubject<any>;

  private priceUpdateSubject = new Subject<PriceUpdate>();
  public priceUpdates$: Observable<PriceUpdate> = this.priceUpdateSubject.asObservable();

  constructor() {
    this.connectToServer();
  }

  /**
   * Connect to Spring Boot WebSocket Server
   */
  private connectToServer() {
    console.log('🔌 Connecting to WebSocket server...');

    this.coinbaseSocket$ = webSocket({
      url: this.WS_ENDPOINT,
      deserializer: msg => JSON.parse(msg.data), // ✅ Auto-parse incoming JSON
    });

    this.coinbaseSocket$.pipe(
      catchError(error => {
        console.error('❌ WebSocket error:', error);
        this.scheduleReconnect();
        throw error;
      }),
      retryWhen(errors => errors.pipe(
        switchMap(() => timer(this.reconnectInterval)) // Retry after delay
      ))
    ).subscribe({
      next: (message: PriceUpdate) => this.handleIncomingMessage(message),
      error: err => this.handleError(err),
      complete: () => {
        console.warn('⚠️ WebSocket connection closed, attempting reconnect...');
        this.scheduleReconnect();
      }
    });
  }

  /**
   * Handle incoming messages from WebSocket server
   */
  private handleIncomingMessage(message: PriceUpdate) {
    console.log('📩 Received message:', message);
    this.priceUpdateSubject.next(message);
  }

  /**
   * Handle WebSocket errors and trigger reconnection
   */
  private handleError(err: any) {
    console.error('🚨 WebSocket error:', err);
    this.scheduleReconnect();
  }

  /**
   * Schedule a WebSocket reconnection after a delay
   */
  private scheduleReconnect() {
    console.log(`🔄 Reconnecting in ${this.reconnectInterval / 1000} seconds...`);
    setTimeout(() => this.connectToServer(), this.reconnectInterval);
  }

  /**
   * Gracefully close WebSocket when the service is destroyed
   */
  ngOnDestroy() {
    console.log('🔌 Closing WebSocket connection...');
    this.coinbaseSocket$?.complete();
  }
}
