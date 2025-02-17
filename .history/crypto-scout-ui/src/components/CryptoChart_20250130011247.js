import React, { useEffect, useRef, useState } from "react";
import { Chart, registerables } from "chart.js";
import connectWebSocket from "../cryptoWebSocket";

Chart.register(...registerables);

const CryptoChart = () => {
  const pricesRef = useRef({ BTC: [], ETH: [], USDT: [], BNB: [], SOL: [] }); // ✅ Store prices in useRef (no re-render)
  const [prices, setPrices] = useState(pricesRef.current);
  const [timestamps, setTimestamps] = useState([]);
  const chartRef = useRef(null);
  const chartInstanceRef = useRef(null);

  useEffect(() => {
    const socket = connectWebSocket((data) => {
      Object.keys(data).forEach((coin) => {
        if (!pricesRef.current[coin]) pricesRef.current[coin] = [];
        pricesRef.current[coin].push(data[coin]);
        if (pricesRef.current[coin].length > 20) pricesRef.current[coin].shift();
      });

      setTimestamps((prev) => [...prev, new Date().toLocaleTimeString()].slice(-20));
    });

    // ✅ Update state every 2 seconds instead of every WebSocket message
    const interval = setInterval(() => {
      setPrices({ ...pricesRef.current });
    }, 2000);

    return () => {
      clearInterval(interval);
      socket.close();
    };
  }, []);

  useEffect(() => {
    if (chartInstanceRef.current) {
      chartInstanceRef.current.destroy();
    }

    const newChartInstance = new Chart(chartRef.current, {
      type: "line",
      data: {
        labels: timestamps,
        datasets: Object.entries(prices).map(([coin, priceData], index) => ({
          label: coin,
          data: priceData,
          borderColor: ["red", "blue", "green", "orange", "purple"][index],
          fill: false,
        })),
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: { type: "category", title: { display: true, text: "Time" } },
          y: { beginAtZero: false, title: { display: true, text: "Price (USD)" } },
        },
      },
    });

    chartInstanceRef.current = newChartInstance;

    return () => {
      if (chartInstanceRef.current) {
        chartInstanceRef.current.destroy();
      }
    };
  }, [timestamps]); // ✅ Only re-render when timestamps change

  return <canvas ref={chartRef} style={{ width: "100%", height: "400px" }} />;
};

export default CryptoChart;
