#!/bin/bash

source config.sh

echo "Terminating and deleting system..."

# Terminate load balancer
"$SCRIPT_DIR"/terminate-system.sh

# Deregister lambda functions
"$SCRIPT_DIR"/function-deregister.sh

# Deregister image
"$SCRIPT_DIR"/deregister-image.sh

echo "Terminated and deleted system..."