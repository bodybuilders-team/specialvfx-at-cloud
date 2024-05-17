#!/bin/bash

source config.sh

# Create load balancer and configure health check.
aws elb create-load-balancer \
	--load-balancer-name CNV-LoadBalancer \
	--listeners "Protocol=HTTP,LoadBalancerPort=80,InstanceProtocol=HTTP,InstancePort=8000" \
	--availability-zones eu-west-3c

aws elb configure-health-check \
	--load-balancer-name CNV-LoadBalancer \
	--health-check Target=HTTP:8000/test,Interval=30,UnhealthyThreshold=2,HealthyThreshold=10,Timeout=5

# Create launch configuration.
aws autoscaling create-launch-configuration \
	--launch-configuration-name CNV-LaunchConfiguration \
	--image-id $(cat image.id) \
	--instance-type t2.micro \
	--security-groups $AWS_SECURITY_GROUP \
	--key-name $AWS_KEYPAIR_NAME \
	--instance-monitoring Enabled=true

# Create auto scaling group.
aws autoscaling create-auto-scaling-group \
	--auto-scaling-group-name CNV-AutoScalingGroup \
	--launch-configuration-name CNV-LaunchConfiguration \
	--load-balancer-names CNV-LoadBalancer \
	--availability-zones eu-west-3c \
	--health-check-type ELB \
	--health-check-grace-period 60 \
	--min-size 1 \
	--max-size 3 \
	--desired-capacity 1

# Create scale-out policy
SCALE_OUT_POLICY_ARN=$(aws autoscaling put-scaling-policy \
    --auto-scaling-group-name CNV-AutoScalingGroup \
    --policy-name ScaleOutPolicy \
    --scaling-adjustment 1 \
    --adjustment-type ChangeInCapacity \
    --query 'PolicyARN' \
    --output text)

# Create scale-in policy
SCALE_IN_POLICY_ARN=$(aws autoscaling put-scaling-policy \
    --auto-scaling-group-name CNV-AutoScalingGroup \
    --policy-name ScaleInPolicy \
    --scaling-adjustment -1 \
    --adjustment-type ChangeInCapacity \
    --query 'PolicyARN' \
    --output text)

# Create CloudWatch alarm for high CPU usage and link to scale-out policy
aws cloudwatch put-metric-alarm \
    --alarm-name "HighCPUUsageAlarm" \
    --alarm-description "Alarm when CPU usage exceeds 50%" \
    --metric-name CPUUtilization \
    --namespace AWS/EC2 \
    --statistic Average \
    --period 300 \
    --threshold 50 \
    --comparison-operator GreaterThanThreshold \
    --dimensions Name=AutoScalingGroupName,Value=CNV-AutoScalingGroup \
    --evaluation-periods 1 \
    --alarm-actions $SCALE_OUT_POLICY_ARN \
    --unit Percent

# Create CloudWatch alarm for low CPU usage and link to scale-in policy
aws cloudwatch put-metric-alarm \
    --alarm-name "LowCPUUsageAlarm" \
    --alarm-description "Alarm when CPU usage falls below 20%" \
    --metric-name CPUUtilization \
    --namespace AWS/EC2 \
    --statistic Average \
    --period 300 \
    --threshold 20 \
    --comparison-operator LessThanThreshold \
    --dimensions Name=AutoScalingGroupName,Value=CNV-AutoScalingGroup \
    --evaluation-periods 1 \
    --alarm-actions $SCALE_IN_POLICY_ARN \
    --unit Percent
