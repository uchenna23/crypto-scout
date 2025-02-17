import React, { useEffect, useState } from "react";
import connectWebSocket from "../cryptoWebSocket";
import { Table, TableHead, TableRow, TableCell, TableBody, Paper, Typography } from "@mui/material";

const CryptoDashboard = () => {
  const [cryptoPrices, setCryptoPrices] = useState({});

  useEffect(() => {
    const socket = connectWebSocket((data) => {
      setCryptoPrices((prevPrices) => ({ ...prevPrices, ...data }));
    });

    return () => socket.close();
  }, []);

  return (
    <Paper sx={{ padding: 3, maxWidth: 600, margin: "auto", marginTop: 5 }}>
      <Typography variant="h5" gutterBottom align="center">
        📊 Live Crypto Prices
      </Typography>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Currency</TableCell>
            <TableCell align="right">Price (USD)</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {Object.entries(cryptoPrices).map(([currency, price]) => (
            <TableRow key={currency}>
              <TableCell>{currency}</TableCell>
              <TableCell align="right">${price}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Paper>
  );
};

export default CryptoDashboard;
