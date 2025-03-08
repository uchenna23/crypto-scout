#!/bin/sh
# Use the provided API_URL environment variable, or default to the backend service name on the local network.
: ${API_URL:="http://backend/api"}
echo "Using API_URL: $API_URL"

# Replace the placeholder {{API_URL}} in index.html with the actual API URL.
sed -i "s|{{API_URL}}|$API_URL|g" /usr/share/nginx/html/index.html

# Execute Nginx
exec nginx -g "daemon off;"
