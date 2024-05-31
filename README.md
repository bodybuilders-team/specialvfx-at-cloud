# SpecialVFX@Cloud

The goal of this project is to design and implement a service hosted in an elastic public cloud to support a
hypothetical Special VFX Studio. The service executes computationally-intensive tasks such as generating photo-realistic
images using ray-tracing and applying effects to images using image processing algorithms.

> Cloud Computing and Virtualization project of group 4 - MEIC @ IST 2023/2024.

## Authors

- [110817 André Páscoa](https://github.com/devandrepascoa)
- [110860 André Jesus](https://github.com/andre-j3sus)
- [110893 Nyckollas Brandão](https://github.com/Nyckoka)

@IST<br>
Master in Computer Science and Computer Engineering<br>
Cloud Computing and Virtualization - Group 4<br>
Summer Semester of 2023/2024

## Table of Contents

- [SpecialVFX@Cloud](#specialvfxcloud)
    - [Authors](#authors)
    - [Table of Contents](#table-of-contents)
    - [Overview](#overview)
    - [Architecture](#architecture)
    - [Usage](#usage)
        - [Requirements](#requirements)
        - [Running locally](#running-locally)
            - [Load Balancer and Auto Scaler](#load-balancer-and-auto-scaler)
            - [Web Server](#web-server)
            - [Workloads](#workloads)
        - [Deploying to AWS](#deploying-to-aws)
        - [All AWS scripts](#all-aws-scripts)
        - [Test scripts](#test-scripts)

## Overview

This project provides a simplified Java application designed to handle different types of requests:

* **Render Image**: Returns the rendering of a viewport received as input.
* **Blur Image**: Returns a blurred copy of the input image.
* **Enhance Image**: Returns a copy of the input image with highlighted edges.

These are exposed through a **Web Server** that receives HTTP requests.
The application is designed to be deployed in a cloud environment, where requests are distributed to instances via a
**Load Balancer** and the number of instances can be scaled based on the load via the **Auto Scaler**.
Additionally, the application is instrumented to collect and store metrics about the requests and the execution of the
workloads.

## Architecture

The project contains the following components:

* [`app`](app) - The main application that exposes the functionality of the workloads.
    * [`raytracer`](app/raytracer) - The Raytracer workload.
    * [`imageproc`](app/imageproc) - The BlurImage and EnhanceImage workloads.
    * [`webserver`](app/webserver) - The web server exposing the functionality of the workloads.
* [`instrumentation`](instrumentation) - The instrumentation module that contains the code for collecting metrics with
  Javassist.
* [`mss`](mss) - The Metric Storage System that stores the requests and metrics collected by the instrumentation module.
* [`loadbalancer`](loadbalancer) - The load balancer + auto scaler.
    * The load balancer that distributes the requests to the different instances of the
    application
    * The autoscaler that scales the number of instances of the application based on the load.
* [`scripts`](scripts) - Scripts to automate instrumentation and deployment tasks;
    * [`aws`](scripts/aws) - Scripts to deploy the application to AWS.
    * [`aws_test`](scripts/aws_test) - Scripts to test the application, AWS or otherwise.

## Usage

### Requirements

* Java 17+
* Maven 3.6.3+

### Running locally

1. Make sure your `JAVA_HOME` environment variable is set to Java 17+ distribution
2. Run `mvn clean install` in the root directory to build the application

#### Load Balancer and Auto Scaler

1. To run web server instances in AWS while using the load balancer and autoscaler locally, you need to setup the
   necessary environment variables:
    * *AWS_ACCESS_KEY_ID*
    * *AWS_SECRET_ACCESS_KEY*
    * *AWS_DEFAULT_REGION*
    * *AWS_IMAGE_ID*
    * *BLUR_LAMBDA_ARN*
    * *ENHANCE_LAMBDA_ARN*
    * *RAYTRACER_LAMBDA_ARN*
    * *AWS_SECURITY_GROUP*
2. Run
   `java -cp loadbalancer/target/loadbalancer-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.LoadAndScaleWebServer`
   to start the load balancer and auto scaler.

#### Web Server

1. To store obtained metrics in remote DynamoDB, setup environment variables for *AWS_ACCESS_KEY_ID* and
   *AWS_SECRET_ACCESS_KEY* **with DynamoDB permissions**.
2. Run `mvn exec:exec -f app/webserver` to start the web server using the instrumented classes.
   Alternatively you can use the following
   command: `java -cp app/webserver/target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.webserver.WebServer -javaagent:instrumentation/target/instrumentation-1.0-SNAPSHOT-jar-with-dependencies.jar=RequestAnalyzer:pt.ulisboa.tecnico.cnv.imageproc,boofcv,pt.ulisboa.tecnico.cnv.raytracer:output`

#### Workloads

To run a workload directly, you can use the following commands:

* Ray
  Tracer: `java -cp app/raytracer/target/raytracer-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.raytracer.Main <input-scene-file.txt> <output-file.bmp> 400 300 400 300 0 0 [-tm=<texmap-file.bmp>] [-aa]`
* Blur
  Image: `java -cp app/imageproc/target/imageproc-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler <input-file> <output-file>`
* Enhance
  Image: `java -cp app/imageproc/target/imageproc-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler <input-file> <output-file>`

### Deploying to AWS

To deploy the application/system to AWS, you can use the provided scripts in the `scripts/aws` directory. The scripts
require
the AWS CLI to be installed and configured with the necessary permissions.

1. Configure the `scripts/aws/config.sh` file with the necessary parameters/environment variables.
2. If it's the first time deploying the system, run `./scripts/aws/create-and-launch-system.sh` to create the necessary
   resources in AWS, including the lambda functions, the worker image and the instance running the load balancer and
   autoscaler;
3. If the system is already created, run `./scripts/aws/launch-system.sh` to deploy the load balancer and autoscaler,
   because the image and lambda functions are already created;

To terminate the system, run `./scripts/aws/terminate-system.sh` - this only terminates the instances and the load
balancer. To terminate and delete all resources, run `./scripts/aws/terminate-and-delete-system.sh`.

### All AWS scripts

You can find all the AWS scripts in the `scripts/aws` directory, allowing you to also run tasks individually.

The scripts are:

* `create-and-launch-system.sh` - Creates the necessary resources in AWS and launches the system.
    * Registers lambda functions for the workloads
    * Creates an image (AMI) for the workers (webserver instances).
    * Deploys an instance for load balancer and autoscaler.
* `launch-system.sh` - Launches the system in AWS.
    * Deploys the load balancer and autoscaler.
* `terminate-and-delete-system.sh` - Terminates and deletes the system in AWS.
    * Terminates the load balancer instance and any running webserver instances.
    * Deletes/Deregisters the lambda functions for the workloads.
    * Deletes/Deregisters the image (AMI) for the workers (webserver instances).
* `terminate-system.sh` - Terminates the system in AWS, without deleting the resources (AMI and lambda functions).
    * Terminates the load balancer instance and any running webserver instances.
* `function-register.sh` - Registers the lambda functions and roles for the workloads.
* `function-deregister.sh` - Deregisters the lambda functions and roles for the workloads.
* `create-image.sh` - Creates an image (AMI) for the workers (webserver instances).
    * Launches an instance and installs the necessary software.
    * Tests the webserver software in the instance.
    * Creates an image from the instance.
    * Terminates the temporary instance.
* `deregister-image.sh` - Deletes/deregisters the image (AMI) for the workers (webserver instances).
* `launch-vm.sh` - Launches a VM instance.
* `install-vm.sh` - Installs the necessary webserver software in a VM instance.
* `test-vm.sh` - Tests the webserver software in a VM instance.

Others:

* `launch-deployment.sh` - Launches the deployment of the alternative system in AWS, using the load balancer and
  autoscaler services from AWS instead of our own version.
* `terminate-deployment.sh` - Terminates the deployment of the alternative system in AWS.

### Test scripts

You can find all the test scripts in the `scripts/aws_test` directory, allowing you to run tests on the system.
The scripts are:

* `test-imageproc-01.sh` - Tests the BlurImage workload.
* `test-imageproc-02.sh` - Tests the EnhanceImage workload.
* `test-raytracer-01.sh` - Tests the Raytracer workload.

These can be configured to your liking, and you can create new test scripts based on these too.