import React, { memo } from "react";
import { Table, TableHead, TableRow, TableCell, TableBody, Paper, Typography } from "@mui/material";

const CryptoDashboard = ({ cryptoPrices = {} }) => { // ✅ Ensure cryptoPrices is always an object
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
          {Object.entries(cryptoPrices || {}).map(([currency, price]) => ( // ✅ Ensure data is always valid
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

export default memo(CryptoDashboard); // ✅ Prevent unnecessary re-renders
