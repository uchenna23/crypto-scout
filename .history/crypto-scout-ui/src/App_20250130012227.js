import React, { useState, useEffect, useRef } from "react";
import CryptoDashboard from "./components/CryptoDashboard";
import CryptoChart from "./components/CryptoChart";
import connectWebSocket from "./cryptoWebSocket";
import { Container, Typography } from "@mui/material";

function App() {
  const pricesRef = useRef({});
  const [cryptoPrices, setCryptoPrices] = useState({});
  const [updateTrigger, setUpdateTrigger] = useState(0);

  useEffect(() => {
    const socket = connectWebSocket((data) => {
      Object.assign(pricesRef.current, data); // ✅ Update without re-rendering immediately

      setUpdateTrigger((prev) => prev + 1); // ✅ Only triggers update every 3s
    });

    return () => socket.close();
  }, []);

  useEffect(() => {
    setCryptoPrices({ ...pricesRef.current });
  }, [updateTrigger]); // ✅ Updates state every 3s

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
