#!/bin/bash

source config.sh

# Step 1: launch a vm instance.
$DIR/launch-vm.sh

# Step 2: install software in the VM instance.
$DIR/install-vm.sh

# Step 3: test VM instance.
$DIR/test-vm.sh

# Step 4: create VM image (AIM).
aws ec2 create-image --instance-id $(cat instance.id) --name CNV-Image | jq -r .ImageId > image.id
echo "New VM image with id $(cat image.id)."

# Step 5: Wait for image to become available.
echo "Waiting for image to be ready... (this can take a couple of minutes)"
aws ec2 wait image-available --filters Name=name,Values=CNV-Image
echo "Waiting for image to be ready... done! \o/"

# Step 6: terminate the vm instance.
aws ec2 terminate-instances --instance-ids $(cat instance.id)
