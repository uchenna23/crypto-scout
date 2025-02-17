import React, { useEffect, useRef, useState } from "react";
import { Line } from "react-chartjs-2";
import { Chart } from "chart.js/auto"; // ✅ Import Chart.js for reference
import connectWebSocket from "../cryptoWebSocket";

const CryptoChart = () => {
  const [prices, setPrices] = useState({ BTC: [], ETH: [], USDT: [], BNB: [], SOL: [] });
  const [timestamps, setTimestamps] = useState([]);
  const chartRef = useRef(null);
  const chartInstanceRef = useRef(null); // ✅ Store Chart.js instance

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
      chartInstanceRef.current.destroy(); // ✅ Destroy previous Chart.js instance before re-rendering
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
    });

    chartInstanceRef.current = newChartInstance; // ✅ Save new Chart.js instance

    return () => {
      if (chartInstanceRef.current) {
        chartInstanceRef.current.destroy(); // ✅ Clean up on unmount
      }
    };
  }, [prices, timestamps]);

  return <canvas ref={chartRef} />; // ✅ Use a canvas element instead of <Line />
};

export default CryptoChart;
