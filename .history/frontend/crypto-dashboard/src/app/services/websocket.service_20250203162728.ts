import { Injectable } from '@angular/core';
import { Client, Message } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {
  private client: Client;
  private subject = new Subject<any>();

  constructor() {
    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      debug: (str) => console.log('📝 STOMP:', str), // Debug logs
    });

    this.client.onConnect = () => {
      console.log('✅ Connected to WebSocket');

      this.client.subscribe('/topic/crypto', (message: Message) => {
        console.log('📩 Received:', message.body);
        this.subject.next(JSON.parse(message.body));
      });
    };

    this.client.onStompError = (frame) => {
      console.error('❌ Broker error:', frame.headers['message']);
      console.error('Details:', frame.body);
    };

    this.client.activate();
  }

  public getData(): Observable<any> {
    return this.subject.asObservable();
  }
}
