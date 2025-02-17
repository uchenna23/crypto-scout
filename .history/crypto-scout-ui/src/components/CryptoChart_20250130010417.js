import React, { useEffect, useRef, useState } from "react";
import { Line } from "react-chartjs-2";
import { Chart } from "chart.js/auto";
import connectWebSocket from "../cryptoWebSocket";

const CryptoChart = () => {
  const [prices, setPrices] = useState({ BTC: [], ETH: [], USDT: [], BNB: [], SOL: [] });
  const [timestamps, setTimestamps] = useState([]);
  const chartRef = useRef(null);

  useEffect(() => {
    const socket = connectWebSocket((data) => {
      setPrices((prevPrices) => {
        const updatedPrices = { ...prevPrices };
        Object.keys(data).forEach((coin) => {
          updatedPrices[coin] = [...(updatedPrices[coin] || []), data[coin]];
          if (updatedPrices[coin].length > 20) updatedPrices[coin].shift(); // Keep last 20 updates
        });
        return updatedPrices;
      });

      setTimestamps((prev) => [...prev, new Date().toLocaleTimeString()].slice(-20));
    });

    return () => {
      socket.close();
      if (chartRef.current) {
        chartRef.current.destroy(); // ✅ Destroy previous chart on unmount
      }
    };
  }, []);

  const chartData = {
    labels: timestamps,
    datasets: Object.entries(prices).map(([coin, priceData], index) => ({
      label: coin,
      data: priceData,
      borderColor: ["red", "blue", "green", "orange", "purple"][index],
      fill: false,
    })),
  };

  return <Line ref={chartRef} data={chartData} />;
};

export default CryptoChart;
