import { Injectable } from '@angular/core';
import { Client, Message, StompSubscription, Frame } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
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
    // Dynamically determine WebSocket URL based on the environment
    const backendHost =
      window.location.hostname === 'localhost'
        ? 'localhost:8080' // Use backend port 8080 instead of 8081
        : window.location.hostname;

    const wsUrl = `http://${backendHost}/ws/`;

    console.log('Using WebSocket URL:', wsUrl);

    this.client = new Client({
      brokerURL: wsUrl.replace(/^http/, 'ws'), // Convert HTTP to WebSocket URL
      webSocketFactory: () => new SockJS(wsUrl), // Ensure SockJS compatibility
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (msg) => console.log(`📡 [WebSocket Debug]: ${msg}`),
    });

    this.client.onConnect = (frame: Frame) => {
      console.log('✅ WebSocket connected:', frame);
      this.subscribeToTopic();
    };

    this.client.activate();
  }

  private subscribeToTopic(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }

    this.subscription = this.client.subscribe(
      '/topic/crypto',
      (message: Message) => {
        console.log('📩 Received message:', message.body);
        try {
          const data = JSON.parse(message.body);
          this.subject.next(data);
        } catch (err) {
          console.error('❌ Error parsing message:', err);
        }
      }
    );
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
