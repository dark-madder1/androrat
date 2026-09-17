#!/bin/bash
# SSL/TLS Setup Script for Androrat Server
# This script generates a self-signed certificate for testing purposes.
# For production use, obtain a certificate from a trusted Certificate Authority.

KEYSTORE_FILE="keystore.jks"
KEYSTORE_PASSWORD="changeit"
KEY_ALIAS="androrat-server"
VALIDITY_DAYS=365

echo "=== Androrat Server SSL/TLS Setup ==="
echo ""
echo "This script will generate a self-signed certificate for testing."
echo "WARNING: Self-signed certificates should NOT be used in production!"
echo ""

# Check if keytool is available
if ! command -v keytool &> /dev/null; then
    echo "ERROR: keytool not found. Please install Java JDK."
    exit 1
fi

# Check if keystore already exists
if [ -f "$KEYSTORE_FILE" ]; then
    echo "WARNING: $KEYSTORE_FILE already exists."
    read -p "Do you want to overwrite it? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 0
    fi
    rm -f "$KEYSTORE_FILE"
fi

# Generate keystore with self-signed certificate
echo "Generating keystore with self-signed certificate..."
keytool -genkeypair \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity "$VALIDITY_DAYS" \
    -keystore "$KEYSTORE_FILE" \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEYSTORE_PASSWORD" \
    -dname "CN=Androrat Server, OU=Security, O=Organization, L=City, ST=State, C=US"

if [ $? -eq 0 ]; then
    echo ""
    echo "SUCCESS: Keystore created successfully!"
    echo ""
    echo "Keystore file: $KEYSTORE_FILE"
    echo "Keystore password: $KEYSTORE_PASSWORD"
    echo ""
    echo "To use this keystore, you can either:"
    echo "1. Place it in the same directory as the server JAR"
    echo "2. Specify its location using system properties:"
    echo "   java -Dandrorat.keystore.path=/path/to/keystore.jks \\"
    echo "        -Dandrorat.keystore.password=$KEYSTORE_PASSWORD \\"
    echo "        -jar AndroratServer.jar"
    echo ""
    echo "To disable SSL/TLS (NOT RECOMMENDED):"
    echo "   java -Dandrorat.ssl.enabled=false -jar AndroratServer.jar"
    echo ""
    echo "IMPORTANT: For production use, obtain a certificate from a trusted CA!"
else
    echo "ERROR: Failed to generate keystore."
    exit 1
fi
