#!/bin/bash

source config.sh

echo "Launching load balancer instance..."
aws ec2 run-instances \
	--image-id resolve:ssm:/aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-x86_64-gp2 \
	--instance-type t2.micro \
	--key-name $AWS_KEYPAIR_NAME \
	--security-group-ids $AWS_SECURITY_GROUP \
	--tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=loadbalancer}]" \
	--monitoring Enabled=true | jq -r ".Instances[0].InstanceId" > lb-instance.id
echo "Launched load balancer instance with id $(cat lb-instance.id)."

echo "Waiting for load balancer instance with id $(cat lb-instance.id) to be running."
aws ec2 wait instance-running --instance-ids "$(cat lb-instance.id)"
echo "Load balancer instance with id $(cat lb-instance.id) is now running."

# Extract DNS name.
aws ec2 describe-instances \
  --instance-ids "$(cat lb-instance.id)" | jq -r ".Reservations[0].Instances[0].NetworkInterfaces[0].PrivateIpAddresses[0].Association.PublicDnsName" > lb-instance.dns
echo "Load balancer instance with id $(cat lb-instance.id) has address $(cat lb-instance.dns)."

# Wait for instance to have SSH ready.
while ! nc -z "$(cat lb-instance.dns)" 22; do
  echo "Waiting for $(cat lb-instance.dns):22 (SSH)..."
  sleep 0.5
done
echo "Load balancer instance with id $(cat lb-instance.id) is ready for SSH access."

echo "Installing software in the load balancer instance..."

# Install java.
cmd="sudo yum update -y; sudo yum install java-17-amazon-corretto -y"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAIR_PATH ec2-user@$(cat lb-instance.dns) $cmd

# Download web server jar.
webserver_jar="loadbalancer-1.0.0-SNAPSHOT-jar-with-dependencies.jar"
webserver_jar_path="$PROJECT_DIR/loadbalancer/target/$webserver_jar"

scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAIR_PATH "$webserver_jar_path" ec2-user@$(cat lb-instance.dns):
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAIR_PATH "$SCRIPT_DIR/config.sh" ec2-user@$(cat lb-instance.dns):

# Setup web server to start on instance launch.
run_cmd="source /home/ec2-user/config.sh && export AWS_IMAGE_ID=$(cat image.id) BLUR_LAMBDA_ARN=$(cat blur-lambda-arn) ENHANCE_LAMBDA_ARN=$(cat enhance-lambda-arn) RAYTRACER_LAMBDA_ARN=$(cat raytracer-lambda-arn) && nohup java -cp /home/ec2-user/$webserver_jar pt.ulisboa.tecnico.cnv.LoadAndScaleWebServer > /home/ec2-user/webserver.log 2>&1 &"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAIR_PATH ec2-user@$(cat lb-instance.dns) "$run_cmd"

echo "Installed software in the load balancer instance."
echo "Load balancer instance with id $(cat lb-instance.id) is now ready."