import React, { useState, useEffect } from "react";
import CryptoDashboard from "./components/CryptoDashboard";
import CryptoChart from "./components/CryptoChart";
import connectWebSocket from "./cryptoWebSocket";
import { Container, Typography } from "@mui/material";

function App() {
  const [cryptoPrices, setCryptoPrices] = useState({}); // ✅ Ensure initial state is an object

  useEffect(() => {
    const socket = connectWebSocket((data) => {
      setCryptoPrices((prevPrices) => ({ ...prevPrices, ...data }));
    });

    return () => socket.close();
  }, []);

  return (
    <Container>
      <Typography variant="h3" align="center" gutterBottom>
        🚀 Crypto Live Tracker
      </Typography>
      <CryptoDashboard cryptoPrices={cryptoPrices} />
      <CryptoChart />
    </Container>
  );
}

export default App;
