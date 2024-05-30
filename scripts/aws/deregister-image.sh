#!/bin/bash

source config.sh

aws ec2 deregister-image --image-id "$(cat image.id)"
echo "Deregistered image with id $(cat image.id)."