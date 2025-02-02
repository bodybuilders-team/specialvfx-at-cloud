#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_DIR="$( cd "$SCRIPT_DIR/../../" && pwd )"

export PATH=~/aws-cli-bin:$PATH
export AWS_DEFAULT_REGION=eu-west-3
export AWS_ACCOUNT_ID=<insert here your aws account id>
export AWS_ACCESS_KEY_ID=<insert here your aws access key>
export AWS_SECRET_ACCESS_KEY=<insert here your aws secret access key>
export AWS_EC2_SSH_KEYPAIR_PATH=<path to aws ssh keypair>
export AWS_SECURITY_GROUP=<name of your security group>
export AWS_KEYPAIR_NAME=<name of your aws keypair>

