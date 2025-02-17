// crypto-websocket.service.ts
import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject, webSocket, WebSocketSubject } from 'rxjs';
import { filter, map } from 'rxjs/operators';

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

  // WebSocketSubject from RxJS handles the low-level WebSocket
  private coinbaseSocket$?: WebSocketSubject<any>;
  
  // Subject to emit parsed ticker messages
  private priceUpdateSubject = new Subject<PriceUpdate>();
  public priceUpdates$: Observable<PriceUpdate> = this.priceUpdateSubject.asObservable();

  constructor() {
    // Connect when the service is created
    this.connectToServer();
  }

  /**
   * Initialize the WebSocketSubject and subscribe to messages.
   */
  private connectToServer() {
    this.coinbaseSocket$ = webSocket('ws://localhost:8080/ws'); 
    // ^ Adjust the URL to match your Spring WebSocket endpoint.
    //   If you're using secure wss:// or a different path, update accordingly.

    this.coinbaseSocket$.subscribe({
      next: (message: any) => this.handleIncomingMessage(message),
      error: (err: any) => this.handleError(err),
      complete: () => console.log('WebSocket connection closed')
    });
  }

  /**
   * Process any incoming messages from the server.
   */
  private handleIncomingMessage(message: any) {
    // The server might be sending a JSON string like: {"BTC-USD": 12345.67}
    // If it's a string, parse it:
    let parsed: PriceUpdate;
    if (typeof message === 'string') {
      parsed = JSON.parse(message);
    } else {
      parsed = message; // If the server is already sending an object, use it directly
    }
    // Emit the parsed price update
    this.priceUpdateSubject.next(parsed);
  }

  /**
   * Handle WebSocket errors.
   */
  private handleError(err: any) {
    console.error('WebSocket error', err);
    // Optionally implement reconnection logic here
  }

  /**
   * If the service is destroyed, close the WebSocket.
   */
  ngOnDestroy() {
    this.coinbaseSocket$?.complete(); // gracefully close the connection
  }
}
