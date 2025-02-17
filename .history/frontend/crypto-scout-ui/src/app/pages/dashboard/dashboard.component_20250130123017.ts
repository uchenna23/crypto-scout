import { Component, OnInit } from '@angular/core';
import { CryptoWebsocketService, PriceUpdate } from '../../services/crypto-websocket.service';
import { Observable } from 'rxjs';
import { CommonModule } from '@angular/common';


@Component({
  standalone: true,
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  imports: [CommonModule]
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
