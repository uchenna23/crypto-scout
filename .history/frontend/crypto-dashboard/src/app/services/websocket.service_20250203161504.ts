import { Injectable } from '@angular/core';
import { Client, Message } from '@stomp/stompjs';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {
  private client: Client;
  private subject = new Subject<any>();

  constructor() {
    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000
    });

    this.client.onConnect = () => {
      console.log('✅ Connected to WebSocket Server');
      this.client.subscribe('/topic/crypto', (message: Message) => {
        console.log('📩 Received:', message.body);
        this.subject.next(JSON.parse(message.body));
      });
    };

    this.client.onStompError = (error) => {
      console.error('❌ STOMP Error:', error);
    };

    this.client.activate();
  }

  public getData(): Observable<any> {
    return this.subject.asObservable();
  }
}
