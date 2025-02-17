import { Injectable } from '@angular/core';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';

export interface PriceUpdate {
  [productId: string]: number;
}

@Injectable({
  providedIn: 'root'
})
export class CryptoWebsocketService {

  private WS_ENDPOINT = 'http://localhost:8080/ws'; // Spring Boot WebSocket URL
  private stompClient!: Client;
  private priceUpdateSubject = new Subject<PriceUpdate>();
  public priceUpdates$: Observable<PriceUpdate> = this.priceUpdateSubject.asObservable();

  constructor() {
    // Connect once, remain connected as long as the service is alive
    this.connectToServer();
  }

  /**
   * Connect to Spring Boot WebSocket using SockJS and STOMP
   */
  private connectToServer() {
    console.log('🔌 Connecting to WebSocket server via SockJS...');

    const socket = new SockJS(this.WS_ENDPOINT);
    this.stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000, // Auto-reconnect every 5 seconds
      debug: (msg) => console.log('🔧 STOMP Debug:', msg),
    });

    this.stompClient.onConnect = () => {
      console.log('✅ Connected to WebSocket');
      // Subscribe to topic
      this.stompClient.subscribe('/topic/crypto', (message) => {
        const parsedMessage: PriceUpdate = JSON.parse(message.body);
        console.log('📩 Received message:', parsedMessage);
        this.priceUpdateSubject.next(parsedMessage);
      });
    };

    this.stompClient.onStompError = (frame) => {
      console.error('🚨 STOMP Error:', frame);
    };

    // Activate the STOMP client
    this.stompClient.activate();
  }

  /**
   * Expose a way to disconnect if truly needed.
   * Otherwise, leave it connected so your app
   * continues receiving WebSocket updates.
   */
  public disconnect() {
    if (this.stompClient && this.stompClient.active) {
      console.log('🔌 Manually disconnecting WebSocket connection...');
      this.stompClient.deactivate();
    }
  }
}
