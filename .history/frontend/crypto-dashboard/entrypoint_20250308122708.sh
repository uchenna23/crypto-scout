#!/bin/sh
# Use the provided API_URL environment variable, or default to the backend service name on the local network.
: ${API_URL:="https://crypto-scout-app.azurewebsites.net/api"}
echo "Using API_URL: $API_URL"

# Use the provided WS_URL environment variable, or default to the backend WebSocket endpoint.
: ${WS_URL:="ws://backend/ws"}
echo "Using WS_URL: $WS_URL"

# Replace the placeholder {{API_URL}} in index.html with the actual API URL.
sed -i "s|{{API_URL}}|$API_URL|g" /usr/share/nginx/html/index.html

# Replace the placeholder {{WS_URL}} in index.html with the actual WS URL.
sed -i "s|{{WS_URL}}|$WS_URL|g" /usr/share/nginx/html/index.html

# Execute Nginx in the foreground.
exec nginx -g "daemon off;"
