#!/bin/bash

source config.sh

echo "Deploying SpecialVFX@Cloud (creating and launching system)..."

# Register lambda functions.
echo "Registering functions on AWS..."
"$SCRIPT_DIR"/function-register.sh

# Create image of the worker.
echo "Creating image of the worker..."
"$SCRIPT_DIR"/create-image.sh

# Deploy load balancer and auto scaler.
echo "Deploying load balancer and auto scaler..."
"$SCRIPT_DIR"/launch-system.sh

echo "SpecialVFX@Cloud deployed."
