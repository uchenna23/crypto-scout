import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';

export interface PriceUpdate {
  [productId: string]: number;
}

@Injectable({
  providedIn: 'root'
})
export class CryptoWebsocketService implements OnDestroy {
  private readonly WS_ENDPOINT = 'http://localhost:8080/ws';
  private stompClient!: Client;
  private priceUpdateSubject = new Subject<PriceUpdate>();
  public priceUpdates$: Observable<PriceUpdate> = this.priceUpdateSubject.asObservable();

  constructor() {
    this.connectToServer();
  }

  /**
   * Connect to the WebSocket server via SockJS and STOMP
   */
  private connectToServer(): void {
    console.log('🔌 Connecting to WebSocket server via SockJS...');

    const socket = new SockJS(this.WS_ENDPOINT);
    this.stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000, // Auto-reconnect every 5s
      heartbeatIncoming: 10000, // Heartbeat to detect connection loss
      heartbeatOutgoing: 10000,
      debug: (msg: string) => console.log('🔧 STOMP Debug:', msg),
    });

    this.stompClient.onConnect = () => {
      console.log('✅ Connected to WebSocket');
      this.subscribeToTopic();
    };

    this.stompClient.onStompError = (frame) => {
      console.error('🚨 STOMP Error:', frame);
    };

    this.stompClient.activate();
  }

  /**
   * Subscribe to the WebSocket topic
   */
  private subscribeToTopic(): void {
    if (this.stompClient && this.stompClient.connected) {
      console.log('📡 Subscribing to /topic/crypto...');
      this.stompClient.subscribe('/topic/crypto', (message: IMessage) => {
        try {
          const parsedMessage: PriceUpdate = JSON.parse(message.body);
          console.log('📩 Received message:', parsedMessage);
          this.priceUpdateSubject.next(parsedMessage);
        } catch (error) {
          console.error('❌ Error parsing message:', error);
        }
      });
    } else {
      console.warn('⚠️ STOMP client is not connected. Retrying...');
      setTimeout(() => this.subscribeToTopic(), 2000);
    }
  }

  /**
   * Manually disconnect from the WebSocket server
   */
  public disconnect(): void {
    if (this.stompClient && this.stompClient.active) {
      console.log('🔌 Manually disconnecting WebSocket connection...');
      this.stompClient.deactivate();
    }
  }

  /**
   * Handle cleanup when service is destroyed
   */
  ngOnDestroy(): void {
    this.disconnect();
  }
}
