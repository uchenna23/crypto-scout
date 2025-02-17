import { Component, OnInit } from '@angular/core';
import { CryptoWebsocketService, PriceUpdate } from '../../services/crypto-websocket.service';
import { Observable } from 'rxjs';

@Component({
  standalone: true,
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {

  priceUpdates$!: Observable<PriceUpdate>;

  constructor(private cryptoService: CryptoWebsocketService) {}

  ngOnInit(): void {
    // Subscribe to the public observable exposed by the service
    this.priceUpdates$ = this.cryptoService.priceUpdates$;
  }
  getKeys(obj: any): string[] {
    return Object.keys(obj);
  }
  
}
