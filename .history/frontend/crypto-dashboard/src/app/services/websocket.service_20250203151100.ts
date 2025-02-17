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
    this.ws = new WebSocket('ws://localhost:8080/ws/crypto');

    this.ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      this.subject.next(data);
    };

    this.ws.onerror = (error) => console.error('WebSocket Error:', error);
    this.ws.onclose = () => {
      console.log('WebSocket closed. Reconnecting in 5 seconds...');
      setTimeout(() => this.connect(), 5000);
    };
  }

  public getData(): Observable<any> {
    return this.subject.asObservable();
  }
}
