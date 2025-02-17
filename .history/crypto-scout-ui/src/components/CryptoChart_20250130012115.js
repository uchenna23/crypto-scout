import React, { useEffect, useRef, useState } from "react";
import { Chart, registerables } from "chart.js";
import connectWebSocket from "../cryptoWebSocket";

Chart.register(...registerables);

const CryptoChart = () => {
  const pricesRef = useRef({ BTC: [], ETH: [], USDT: [], BNB: [], SOL: [] });
  const timestampsRef = useRef([]);
  const chartRef = useRef(null);
  const chartInstanceRef = useRef(null);

  const [updateTrigger, setUpdateTrigger] = useState(0); // ✅ Triggers updates every 3s

  useEffect(() => {
    const socket = connectWebSocket((data) => {
      Object.keys(data).forEach((coin) => {
        if (!pricesRef.current[coin]) pricesRef.current[coin] = [];
        pricesRef.current[coin].push(data[coin]);
        if (pricesRef.current[coin].length > 20) pricesRef.current[coin].shift();
      });

      timestampsRef.current.push(new Date().toLocaleTimeString());
      if (timestampsRef.current.length > 20) timestampsRef.current.shift();

      setUpdateTrigger((prev) => prev + 1); // ✅ Only triggers updates every 3s
    });

    return () => socket.close();
  }, []);

  useEffect(() => {
    if (chartInstanceRef.current) {
      chartInstanceRef.current.destroy();
    }

    const newChartInstance = new Chart(chartRef.current, {
      type: "line",
      data: {
        labels: timestampsRef.current,
        datasets: Object.entries(pricesRef.current).map(([coin, priceData], index) => ({
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
  }, [updateTrigger]); // ✅ Only updates every 3s

  return <canvas ref={chartRef} style={{ width: "100%", height: "400px" }} />;
};

export default CryptoChart;
