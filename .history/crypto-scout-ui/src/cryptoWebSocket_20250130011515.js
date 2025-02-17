const connectWebSocket = (onMessage) => {
  const socket = new WebSocket("ws://localhost:8080/ws/crypto");

  socket.onopen = () => console.log("✅ Connected to WebSocket");
  socket.onmessage = (event) => {
    const data = JSON.parse(event.data);
    localStorage.setItem("cryptoPrices", JSON.stringify(data)); // ✅ Save latest data locally
    onMessage(data);
  };
  socket.onerror = (error) => console.error("WebSocket Error: ", error);
  socket.onclose = () => {
    console.log("⚠️ WebSocket closed. Reconnecting in 5s...");
    setTimeout(() => connectWebSocket(onMessage), 5000);
  };

  return socket;
};

export default connectWebSocket;
