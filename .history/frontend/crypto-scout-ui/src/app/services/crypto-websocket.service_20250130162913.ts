import { Injectable } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs'; // ✅ Import IMessage
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';

export interface PriceUpdate {
  [productId: string]: number;
}

@Injectable({
  providedIn: 'root'
})
export class CryptoWebsocketService {
  private WS_ENDPOINT = 'http://localhost:8080/ws';
  private stompClient!: Client;
  private priceUpdateSubject = new Subject<PriceUpdate>();
  public priceUpdates$: Observable<PriceUpdate> = this.priceUpdateSubject.asObservable();

  constructor() {
    this.connectToServer();
  }

  private connectToServer() {
    console.log('🔌 Connecting to WebSocket server via SockJS...');

    const socket = new SockJS(this.WS_ENDPOINT);
    this.stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000, // Auto-reconnect every 5s
      debug: (msg: string) => console.log('🔧 STOMP Debug:', msg), // ✅ Explicit type
    });

    this.stompClient.onConnect = () => {
      console.log('✅ Connected to WebSocket');
      this.stompClient.subscribe('/topic/crypto', (message: IMessage) => { // ✅ Explicit type
        const parsedMessage: PriceUpdate = JSON.parse(message.body);
        console.log('📩 Received message:', parsedMessage);
        this.priceUpdateSubject.next(parsedMessage);
      });
    };

    this.stompClient.onStompError = (frame: any) => { // ✅ Fix missing type
      console.error('🚨 STOMP Error:', frame);
    };

    this.stompClient.activate();
  }

  disconnect() {
    if (this.stompClient && this.stompClient.active) {
      console.log('🔌 Manually disconnecting WebSocket connection...');
      this.stompClient.deactivate();
    }
  }
}
