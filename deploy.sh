#!/bin/bash

# Deploy script for SimplyPages demo to remote VM
# Usage: ./deploy.sh <username> <ip> <password>

set -euo pipefail  # Exit on error or unset variables

# Check arguments
if [ $# -ne 3 ]; then
    echo "Usage: $0 <username> <ip> <password>"
    echo "Example: $0 demo 192.168.1.100 mypassword"
    exit 1
fi

SSH_USER="$1"
SSH_IP="$2"
SSH_PASS="$3"
SSH_HOST="${SSH_USER}@${SSH_IP}"
REMOTE_PATH="/home/${SSH_USER}"

echo "=== SimplyPages Remote Deployment Script ==="
echo "Target: $SSH_HOST"
echo "Remote path: $REMOTE_PATH"
echo ""

# Check if sshpass is installed
if ! command -v sshpass &> /dev/null; then
    echo "ERROR: sshpass is not installed"
    echo "Install with: sudo dnf install sshpass  (or sudo apt-get install sshpass)"
    exit 1
fi

# Generate Javadocs and Build
echo "Step 1: Generating Javadocs..."
./mvnw clean javadoc:javadoc -pl simplypages
echo "✓ Javadocs generated"
echo ""

echo "Step 2: Copying Javadocs to demo..."
mkdir -p demo/src/main/resources/static/javadocs
cp -r simplypages/target/reports/apidocs/* demo/src/main/resources/static/javadocs/
echo "✓ Javadocs copied"
echo ""

echo "Step 3: Testing and building project locally..."
./mvnw package
echo "✓ Build complete"
echo ""

# Select built demo jar
echo "Step 4: Locating built demo jar..."
shopt -s nullglob
all_jars=(demo/target/*.jar)
shopt -u nullglob
demo_jars=()
for jar in "${all_jars[@]}"; do
    if [[ "$jar" != *.jar.original ]]; then
        demo_jars+=("$jar")
    fi
done
if [ ${#demo_jars[@]} -eq 0 ]; then
    echo "ERROR: No demo jar found in demo/target/"
    exit 1
fi
if [ ${#demo_jars[@]} -gt 1 ]; then
    echo "WARNING: Multiple demo jars found, using: ${demo_jars[0]}"
fi
JAR_PATH="${demo_jars[0]}"
echo "✓ Using jar: $JAR_PATH"
echo ""

echo "Step 5: Transferring jar to remote..."
sshpass -p "$SSH_PASS" scp -o StrictHostKeyChecking=no \
    "$JAR_PATH" \
    "$SSH_HOST:$REMOTE_PATH/"
echo "✓ Jar transferred"
echo ""

echo "Step 6: Restarting remote service..."
sshpass -p "$SSH_PASS" ssh -o StrictHostKeyChecking=no "$SSH_HOST" \
    "cd $REMOTE_PATH && { ./sp_demo.sh stop || true; ./sp_demo.sh start; }"
echo "✓ Remote service restarted"

echo ""
echo "=== Deployment Script Finished ==="
echo "Remote host: $SSH_HOST"
echo "Remote path: $REMOTE_PATH"
