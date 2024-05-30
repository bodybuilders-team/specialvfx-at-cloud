#!/bin/bash

source config.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
source "$SCRIPT_DIR"/config.sh

aws lambda delete-function --function-name blur-lambda
aws lambda delete-function --function-name enhance-lambda
aws lambda delete-function --function-name raytracer-lambda
echo "Deleted/deregistered lambda functions."

aws iam detach-role-policy \
	--role-name lambda-role \
	--policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
echo "Detached policies of lambda-role."

aws iam delete-role --role-name lambda-role
echo "Detached lambda-role."