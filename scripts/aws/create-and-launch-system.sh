#!/bin/bash

source config.sh

# Register lambda functions.
echo "Registering functions on AWS..."
"$SCRIPT_DIR"/function-register.sh

# Create image of the worker.
echo "Creating image of the worker..."
"$SCRIPT_DIR"/create-image.sh

# Deploy load balancer and auto scaler.
echo "Deploying load balancer and auto scaler..."
"$SCRIPT_DIR"/launch-lb.sh

echo "SpecialVFX@Cloud deployed."
