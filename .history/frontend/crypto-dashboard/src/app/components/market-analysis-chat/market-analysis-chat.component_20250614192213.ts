import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-market-analysis-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule, ButtonModule],
  templateUrl: './market-analysis-chat.component.html',
  styleUrls: ['./market-analysis-chat.component.scss']
})
export class MarketAnalysisChatComponent {
  messages: { text: string, sender: 'user' | 'bot' }[] = [];
  userInput: string = '';
  private apiUrl = 'http://localhost:8081/api/openai/chat';

  constructor(private http: HttpClient) {}

  sendMessage() {
    if (!this.userInput.trim()) return;

    // Add user message
    this.messages.push({ text: this.userInput, sender: 'user' });

    const userMessage = this.userInput; // Store message before clearing input
    this.userInput = '';

    // Call backend OpenAI analysis service
    this.http.post(this.apiUrl, { cryptoData: userMessage }, { responseType: 'text' })
      .subscribe(
        response => {
          // Since we're expecting plain text, push it directly.
          this.messages.push({ text: response, sender: 'bot' });
        },
        error => {
          this.messages.push({ text: "Error fetching analysis. Please try again.", sender: 'bot' });
        }
      );
  }
}
