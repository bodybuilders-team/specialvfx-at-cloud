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
aws ec2 run-instances \
	--image-id resolve:ssm:/aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-x86_64-gp2 \
	--instance-type t2.micro \
	--key-name $AWS_KEYPAIR_NAME \
	--security-group-ids $AWS_SECURITY_GROUP \
	--monitoring Enabled=true | jq -r ".Instances[0].InstanceId" > lb-instance.id

aws ec2 wait instance-running --instance-ids $(cat lb-instance.id)
echo "Load balancer instance with id $(cat lb-instance.id) is now running."

# Extract DNS name.
aws ec2 describe-instances \
  --instance-ids $(cat lb-instance.id) | jq -r ".Reservations[0].Instances[0].NetworkInterfaces[0].PrivateIpAddresses[0].Association.PublicDnsName" > lb-instance.dns
echo "Load balancer instance with id $(cat lb-instance.id) has address $(cat lb-instance.dns)."

# Wait for instance to have SSH ready.
while ! nc -z $(cat lb-instance.dns) 22; do
  echo "Waiting for $(cat lb-instance.dns):22 (SSH)..."
  sleep 0.5
done
echo "Load balancer instance with id $(cat lb-instance.id) is ready for SSH access."

echo "SpecialVFX@Cloud deployed."

