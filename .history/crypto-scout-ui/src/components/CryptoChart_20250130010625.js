import React, { useEffect, useRef, useState } from "react";
import { Line } from "react-chartjs-2";
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
          if (updatedPrices[coin].length > 20) updatedPrices[coin].shift();
        });
        return updatedPrices;
      });

      setTimestamps((prev) => [...prev, new Date().toLocaleTimeString()].slice(-20));
    });

    return () => socket.close();
  }, []);

  useEffect(() => {
    const chartInstance = chartRef.current; // ✅ Store the ref in a variable
    return () => {
      if (chartInstance) {
        chartInstance.destroy(); // ✅ Safely destroy the chart
      }
    };
  }, [prices, timestamps]);

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
