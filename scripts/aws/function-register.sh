#!/bin/bash

source config.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
source "$SCRIPT_DIR"/config.sh

echo "Creating role for lambda functions..."
aws iam create-role \
	--role-name lambda-role \
	--assume-role-policy-document '{"Version": "2012-10-17","Statement": [{ "Effect": "Allow", "Principal": {"Service": "lambda.amazonaws.com"}, "Action": "sts:AssumeRole"}]}' > /dev/null

sleep 5

echo "Attaching policies to lambda-role..."
aws iam attach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

sleep 5

# Create lambda function for imageproc
echo "Creating lambda for imageproc..."
aws lambda create-function \
	--function-name blur-lambda \
	--zip-file fileb://../../app/imageproc/target/imageproc-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler \
	--runtime java17 \
	--timeout 300 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role | jq -r '.FunctionArn' > blur-lambda-arn

aws lambda create-function \
	--function-name enhance-lambda \
	--zip-file fileb://../../app/imageproc/target/imageproc-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
	--handler pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler \
	--runtime java17 \
	--timeout 300 \
	--memory-size 256 \
	--role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role | jq -r '.FunctionArn' > enhance-lambda-arn

# Create lambda function for raytracer
echo "Creating lambda for raytracer..."
aws lambda create-function \
  --function-name raytracer-lambda \
  --zip-file fileb://../../app/raytracer/target/raytracer-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
  --handler pt.ulisboa.tecnico.cnv.raytracer.RaytracerHandler \
  --runtime java17 \
	--timeout 300 \
  --memory-size 256 \
  --role arn:aws:iam::$AWS_ACCOUNT_ID:role/lambda-role | jq -r '.FunctionArn' > raytracer-lambda-arn

echo "Functions registered on AWS."