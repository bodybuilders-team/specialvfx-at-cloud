#!/bin/bash

source config.sh

# Terminate load balancer
aws ec2 terminate-instances --instance-ids "$(cat lb-instance.id)" > /dev/null
echo "Terminated load balancer instance with id $(cat lb-instance.id)."

# Terminate all instances
INSTANCE_IDS=$(aws ec2 describe-instances --filters "Name=image-id,Values=$(cat image.id)" --query "Reservations[].Instances[].InstanceId" --output text)
if [ -n "$INSTANCE_IDS" ]; then
    aws ec2 terminate-instances --instance-ids $INSTANCE_IDS > /dev/null
    echo "Terminating instances: $INSTANCE_IDS"
else
    echo "No instances found with image ID $(cat image.id)"
fi
