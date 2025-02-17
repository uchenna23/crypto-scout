import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import SockJS from 'sockjs-client';
import { Client, over } from 'stompjs';

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
    const socket = new SockJS('http://localhost:8080/ws');
    this.client = over(socket);

    this.client.connect({}, () => {
      console.log('✅ WebSocket connected to backend');
      this.client.subscribe('/topic/crypto', (message: { body: string; }) => {
        console.log('📩 Received message:', message.body);
        const data = JSON.parse(message.body);
        this.subject.next(data);
      });
    }, (error: any) => {
      console.error('🚨 WebSocket error:', error);
    });
  }

  public getData(): Observable<any> {
    return this.subject.asObservable();
  }
}
