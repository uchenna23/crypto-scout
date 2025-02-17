import React from "react";
import CryptoDashboard from "./components/CryptoDashboard";
import CryptoChart from "./components/CryptoChart";
import { Container, Typography } from "@mui/material";

function App() {
  return (
    <Container>
      <Typography variant="h3" align="center" gutterBottom>
        🚀 Crypto Live Tracker
      </Typography>
      <CryptoDashboard />
      <CryptoChart />
    </Container>
  );
}

export default App;
