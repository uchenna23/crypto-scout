import React, { useEffect, useRef, useState } from "react";
import { Chart, registerables } from "chart.js"; // ✅ Import registerables
import connectWebSocket from "../cryptoWebSocket";

// ✅ Register all required components (scales, elements, controllers)
Chart.register(...registerables);

const CryptoChart = () => {
  const [prices, setPrices] = useState({ BTC: [], ETH: [], USDT: [], BNB: [], SOL: [] });
  const [timestamps, setTimestamps] = useState([]);
  const chartRef = useRef(null);
  const chartInstanceRef = useRef(null); // Store Chart.js instance

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
    if (chartInstanceRef.current) {
      chartInstanceRef.current.destroy(); // ✅ Destroy previous Chart.js instance
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
          x: { type: "category", title: { display: true, text: "Time" } }, // ✅ Fix category scale registration
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
  }, [prices, timestamps]);

  return <canvas ref={chartRef} style={{ width: "100%", height: "400px" }} />;
};

export default CryptoChart;
