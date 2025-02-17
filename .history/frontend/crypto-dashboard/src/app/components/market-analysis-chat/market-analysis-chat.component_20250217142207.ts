import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-market-analysis-chat',
  templateUrl: './market-analysis-chat.component.html',
  styleUrls: ['./market-analysis-chat.component.scss']
})
export class MarketAnalysisChatComponent {
  messages: { text: string, sender: 'user' | 'bot' }[] = [];
  userInput: string = '';
  private apiUrl = 'http://localhost:8080/api/openai/analyze';

  constructor(private http: HttpClient) {}

  sendMessage() {
    if (!this.userInput.trim()) return;

    this.messages.push({ text: this.userInput, sender: 'user' });

    const userMessage = this.userInput;
    this.userInput = '';

    // Call backend OpenAI analysis service
    this.http.post<{ response: string }>(this.apiUrl, { cryptoData: userMessage }).subscribe(
      response => {
        this.messages.push({ text: response.response, sender: 'bot' });
      },
      error => {
        this.messages.push({ text: "Error fetching analysis. Please try again.", sender: 'bot' });
      }
    );
  }
}
