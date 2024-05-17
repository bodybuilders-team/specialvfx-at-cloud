#!/bin/bash

source config.sh

# Install java.
cmd="sudo yum update -y; sudo yum install java-17-amazon-corretto -y"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAIR_PATH ec2-user@$(cat instance.dns) $cmd

# Download web server and instrumentation jars.
webserver_jar="webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar"
instrumentation_jar="instrumentation-1.0-SNAPSHOT-jar-with-dependencies.jar"
webserver_jar_path="$PROJECT_DIR/app/webserver/target/$webserver_jar"
instrumentation_jar_path="$PROJECT_DIR/instrumentation/target/$instrumentation_jar"

scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAIR_PATH "$webserver_jar_path" ec2-user@$(cat instance.dns):
scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAIR_PATH "$instrumentation_jar_path" ec2-user@$(cat instance.dns):

scp -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAIR_PATH "$SCRIPT_DIR/config.sh" ec2-user@$(cat instance.dns):

# Setup web server to start on instance launch.
run_cmd="source /home/ec2-user/config.sh && java -cp /home/ec2-user/$webserver_jar -javaagent:/home/ec2-user/$instrumentation_jar=RequestAnalyzer:pt.ulisboa.tecnico.cnv.imageproc,pt.ulisboa.tecnico.cnv.raytracer:output pt.ulisboa.tecnico.cnv.webserver.WebServer > /home/ec2-user/webserver.log 2>&1"
cmd="echo \"$run_cmd\" | sudo tee -a /etc/rc.local; sudo chmod +x /etc/rc.local"
ssh -o StrictHostKeyChecking=no -i $AWS_EC2_SSH_KEYPAIR_PATH ec2-user@$(cat instance.dns) $cmd
