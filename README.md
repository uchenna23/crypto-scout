# Crypto Scout

Crypto Scout is a comprehensive cryptocurrency monitoring and analysis platform that provides real-time price dashboards and an AI-powered market analysis chat bot. The project integrates live data from cryptocurrency exchanges with advanced market analysis powered by OpenAI, offering users both visual dashboards and interactive insights.

## Features

- **Live Price Change Dashboard:**  
  Real-time updates of cryptocurrency prices using WebSockets and dynamic charts.

- **Price History Dashboard:**  
  Visualize historical price data with interactive charts, allowing users to track trends over time.

- **Market Analysis Chat Bot:**  
  Ask questions about specific cryptocurrencies and receive AI-generated market analysis. The chat bot leverages the OpenAI API for intelligent insights.

## Technologies Used

- **Backend:**
  - **Spring Boot:** Provides RESTful APIs and WebSocket communication.
  - **Resilience4j:** Implements rate limiting for API calls.
  - **WebSocket:** Streams real-time cryptocurrency data.
  
- **Frontend:**
  - **Angular (Standalone Components):** For building a modular and responsive UI.
  - **PrimeNG:** For UI components such as menus, charts, and buttons.
  - **Chart.js (via PrimeNG UIChart):** For rendering live and historical data visualizations.

- **APIs & Integrations:**
  - **OpenAI API:** Provides AI-driven market analysis.
  - **Coinbase API:** Retrieves cryptocurrency pricing data.

## Installation

### Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **Node.js & npm**
- **Angular CLI 15+**
- **Redis** (for caching)
