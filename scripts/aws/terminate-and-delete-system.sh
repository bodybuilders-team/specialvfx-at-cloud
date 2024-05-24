#!/bin/bash

source config.sh

# Terminate load balancer
"$SCRIPT_DIR"/terminate-system.sh

# Deregister lambda functions
"$SCRIPT_DIR"/function-deregister.sh

# Deregister image
"$SCRIPT_DIR"/deregsiter-image.sh