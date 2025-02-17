import React, { memo } from "react";
import { FixedSizeList as List } from "react-window";
import { Paper, Typography } from "@mui/material";

const Row = ({ index, style, data }) => {
  const [currency, price] = Object.entries(data)[index];
  return (
    <div style={style}>
      <strong>{currency}</strong>: ${price}
    </div>
  );
};

const CryptoDashboard = ({ cryptoPrices = {} }) => {
  const items = Object.entries(cryptoPrices);

  return (
    <Paper sx={{ padding: 3, maxWidth: 600, margin: "auto", marginTop: 5 }}>
      <Typography variant="h5" gutterBottom align="center">
        📊 Live Crypto Prices
      </Typography>
      <List
        height={300} // ✅ Only renders items that fit in 300px
        itemCount={items.length}
        itemSize={35}
        width={500}
        itemData={cryptoPrices}
      >
        {Row}
      </List>
    </Paper>
  );
};

export default memo(CryptoDashboard);
