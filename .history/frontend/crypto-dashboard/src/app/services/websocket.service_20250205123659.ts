import { Injectable } from '@angular/core';
import { Client, over } from 'stompjs';
import * as SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {
  private client!: Client;
  private subject = new Subject<any>();

  constructor() {
    this.connect();
  }

  private connect() {
    const socket = new SockJS.default('http://localhost:8080/ws'); // Backend WebSocket endpoint
    this.client = over(socket);

    this.client.connect({}, () => {
      console.log('✅ WebSocket connected to backend');
      this.client.subscribe('/topic/crypto', (message) => {
        console.log('📩 Received message:', message.body);
        const data = JSON.parse(message.body);
        this.subject.next(data);
      });
    }, (error) => {
      console.error('🚨 WebSocket error:', error);
    });
  }

  public getData(): Observable<any> {
    return this.subject.asObservable();
  }
}
