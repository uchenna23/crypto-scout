import { Injectable, OnDestroy } from '@angular/core';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';

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

  private WS_ENDPOINT = 'http://localhost:8080/ws'; // ✅ Spring Boot WebSocket URL
  private stompClient!: Client;
  private priceUpdateSubject = new Subject<PriceUpdate>();
  public priceUpdates$: Observable<PriceUpdate> = this.priceUpdateSubject.asObservable();

  constructor() {
    this.connectToServer();
  }

  /**
   * Connect to Spring Boot WebSocket using SockJS and STOMP
   */
  private connectToServer() {
    console.log('🔌 Connecting to WebSocket server via SockJS...');

    const socket = new SockJS(this.WS_ENDPOINT); // ✅ Use SockJS instead of raw WebSocket
    this.stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000, // Auto-reconnect every 5s
      debug: (msg) => console.log('🔧 STOMP Debug:', msg),
    });

    this.stompClient.onConnect = () => {
      console.log('✅ Connected to WebSocket');
      this.stompClient.subscribe('/topic/crypto', (message) => {
        const parsedMessage: PriceUpdate = JSON.parse(message.body);
        console.log('📩 Received message:', parsedMessage);
        this.priceUpdateSubject.next(parsedMessage);
      });
    };

    this.stompClient.onStompError = (frame) => {
      console.error('🚨 STOMP Error:', frame);
    };

    this.stompClient.activate();
  }

  /**
   * Gracefully close WebSocket when the service is destroyed
   */
  ngOnDestroy() {
    console.log('🔌 Closing WebSocket connection...');
    this.stompClient?.deactivate();
  }
}
