import { Injectable } from '@angular/core';
import { Client, Message, StompSubscription, Frame } from '@stomp/stompjs';
import * as SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class WebsocketService {
  private client!: Client;
  private subject = new Subject<any>();
  private subscription!: StompSubscription;

  constructor() {
    this.connect();
  }

  private connect(): void {
    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws', // Use WebSocket directly
      webSocketFactory: () => new SockJS.default('http://localhost:8080/ws'), // Fallback to SockJS
      reconnectDelay: 5000, // Reconnect after 5 seconds if disconnected
      heartbeatIncoming: 4000, // Heartbeat settings
      heartbeatOutgoing: 4000,
      debug: (msg) => console.log(`📡 [WebSocket Debug]: ${msg}`),
    });

    this.client.onConnect = (frame: Frame) => {
      console.log('✅ WebSocket connected:', frame);
      this.subscribeToTopic();
    };

    this.client.onStompError = (frame) => {
      console.error('🚨 STOMP Error:', frame.headers['message'], frame.body);
    };

    this.client.onWebSocketError = (error) => {
      console.error('⚠️ WebSocket error:', error);
    };

    this.client.activate();
  }

  private subscribeToTopic(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }

    this.subscription = this.client.subscribe('/topic/crypto', (message: Message) => {
      console.log('📩 Received message:', message.body);
      try {
        const data = JSON.parse(message.body);
        this.subject.next(data);
      } catch (err) {
        console.error('❌ Error parsing message:', err);
      }
    });
  }

  public getData(): Observable<any> {
    return this.subject.asObservable();
  }

  public disconnect(): void {
    if (this.client && this.client.connected) {
      this.client.deactivate();
      console.log('🔌 WebSocket disconnected');
    }
  }
}
