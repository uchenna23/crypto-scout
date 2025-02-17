const connectWebSocket = (onMessage) => {
  const socket = new WebSocket("ws://localhost:8080/ws/crypto");

  socket.onopen = () => console.log("✅ Connected to WebSocket");
  socket.onmessage = (event) => {
    const data = JSON.parse(event.data);
    if (data && typeof data === "object") { // ✅ Ensure data is valid
      onMessage(data);
    } else {
      console.warn("⚠️ Received invalid WebSocket data:", data);
    }
  };
  socket.onerror = (error) => console.error("WebSocket Error: ", error);
  socket.onclose = () => {
    console.log("⚠️ WebSocket closed. Reconnecting in 5s...");
    setTimeout(() => connectWebSocket(onMessage), 5000);
  };

  return socket;
};

export default connectWebSocket;
