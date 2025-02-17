import { Injectable } from '@angular/core';
import { Client, Message } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class WebsocketService {
  private client!: Client;
  private subject = new Subject<any>();

  constructor() {
    this.connect();
  }

  private connect(): void {
    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'), // Backend WebSocket endpoint
      reconnectDelay: 5000, // Reconnect after 5 seconds if the connection drops
      debug: (msg) => console.log(`📡 [WebSocket Debug]: ${msg}`),
    });

    this.client.onConnect = () => {
      console.log('✅ WebSocket connected to backend');
      this.client.subscribe('/topic/crypto', (message: Message) => {
        console.log('📩 Received message:', message.body);
        const data = JSON.parse(message.body);
        this.subject.next(data);
      });
    };

    this.client.onStompError = (error) => {
      console.error('🚨 WebSocket error:', error);
    };

    this.client.activate();
  }

  public getData(): Observable<any> {
    return this.subject.asObservable();
  }
}
