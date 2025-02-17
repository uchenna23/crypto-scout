const connectWebSocket = (onMessage) => {
  const socket = new WebSocket("ws://localhost:8080/ws/crypto");

  let lastMessageTime = Date.now();

  socket.onopen = () => {
    console.log("✅ Connected to WebSocket");
    lastMessageTime = Date.now();
  };

  socket.onmessage = (event) => {
    const data = JSON.parse(event.data);
    lastMessageTime = Date.now(); // ✅ Reset timer on new message
    onMessage(data);
  };

  socket.onerror = (error) => console.error("WebSocket Error: ", error);

  socket.onclose = () => {
    console.log("⚠️ WebSocket closed. Reconnecting in 5s...");
    setTimeout(() => connectWebSocket(onMessage), 5000);
  };

  // ✅ Reconnect if no message is received in the last 65 minutes
  setInterval(() => {
    if (Date.now() - lastMessageTime > 65 * 60 * 1000) {
      console.log("⚠️ No message received in over an hour. Reconnecting...");
      socket.close();
    }
  }, 60000); // Check every 1 minute

  return socket;
};

export default connectWebSocket;
