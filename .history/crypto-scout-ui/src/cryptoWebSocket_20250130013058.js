const connectWebSocket = (onMessage) => {
  const socket = new WebSocket("ws://localhost:8080/ws/crypto");

  let lastMessageTime = Date.now();
  let pendingUpdate = null;

  socket.onopen = () => console.log("✅ Connected to WebSocket");

  socket.onmessage = (event) => {
    const data = JSON.parse(event.data);
    lastMessageTime = Date.now();

    if (pendingUpdate) {
      clearTimeout(pendingUpdate); // ✅ Prevent multiple updates at once
    }

    pendingUpdate = setTimeout(() => {
      onMessage(data); // ✅ Only update state once every 5 seconds
    }, 5000);
  };

  socket.onerror = (error) => console.error("WebSocket Error: ", error);
  socket.onclose = () => {
    console.log("⚠️ WebSocket closed. Reconnecting in 5s...");
    setTimeout(() => connectWebSocket(onMessage), 5000);
  };

  return socket;
};

export default connectWebSocket;
