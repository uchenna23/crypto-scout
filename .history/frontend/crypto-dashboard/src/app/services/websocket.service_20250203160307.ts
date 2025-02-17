import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {
  private ws!: WebSocket;
  private subject = new Subject<any>();

  constructor() {
    this.connect();
  }

  private connect() {
    console.log('🔌 Connecting to WebSocket...');

    this.ws = new WebSocket('ws://localhost:8080/ws/crypto');  // ✅ Ensure this URL matches the backend endpoint

    this.ws.onopen = () => {
      console.log('✅ WebSocket connection established.');
    };

    this.ws.onmessage = (event) => {
      console.log('📡 Data received from backend:', event.data);  // ✅ Debug Log

      try {
        const data = JSON.parse(event.data);
        this.subject.next(data);  // ✅ Send data to subscribers
      } catch (error) {
        console.error('❌ Error parsing WebSocket message:', error);
      }
    };

    this.ws.onerror = (error) => console.error('🚨 WebSocket Error:', error);

    this.ws.onclose = (event) => {
      console.warn(`⚠️ WebSocket closed (Code: ${event.code}). Reconnecting in 5 seconds...`);
      setTimeout(() => this.connect(), 5000);  // ✅ Auto-reconnect
    };
  }

  public getData(): Observable<any> {
    return this.subject.asObservable();
  }
}
