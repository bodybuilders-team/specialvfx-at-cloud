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
        - [Running the Application](#running-the-application)
        - [Deploying to AWS](#deploying-to-aws)

## Overview

This project provides a simplified Java application designed to handle different types of requests:

* **Render Image**: Returns the rendering of a viewport received as input.
* **Blur Image**: Returns a blurred copy of the input image.
* **Enhance Image**: Returns a copy of the input image with highlighted edges.

## Architecture

The project contains the following components:

* [`app`](app) - The main application that exposes the functionality of the workloads.
    * [`raytracer`](app/raytracer) - The Ray Tracing workload.
    * [`imageproc`](app/imageproc) - The BlurImage and EnhanceImage workloads.
    * [`webserver`](app/webserver) - The web server exposing the functionality of the workloads.
* [`instrumentation`](instrumentation) - The instrumentation module that contains the code for collecting metrics with
  Javassist.
* [`mss`](mss) - The Metric Storage System that stores the requests and metrics collected by the instrumentation module.
* [`loadbalancer`](loadbalancer) - The load balancer that distributes the requests to the different instances of the
  application.
* [`autoscaler`](autoscaler) - The autoscaler that scales the number of instances of the application based on the load.
* [`scripts`](scripts) - Scripts to automate instrumentation and deployment tasks;
    * [`aws`](scripts/aws) - Scripts to deploy the application to AWS.

> **⚠️ NOTE**: The load balancer, autoscaler and mss are not totally implemented yet.

## Usage

### Requirements

* Java 17+
* Maven 3.6.3+

### Running the Application

1. Make sure your `JAVA_HOME` environment variable is set to Java 11+ distribution
2. Run `mvn clean install` in the root directory to build the application
3. Run `mvn exec:exec -f app/webserver` to start the web server using the instrumented classes

To run a workload directly, you can use the following commands:

* Ray
  Tracing: `java -cp app/target/raytracer-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.raytracer.WebServer <input-scene-file.txt> <output-file.bmp> 400 300 400 300 0 0 [-tm=<texmap-file.bmp>] [-aa]`
* Blur
  Image: `java -cp app/target/imageproc-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler <input-file> <output-file>`
* Enhance
  Image: `java -cp app/target/imageproc-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler <input-file> <output-file>`

### Deploying to AWS

To deploy the application to AWS, you can use the provided scripts in the `scripts/aws` directory. The scripts require
the AWS CLI to be installed and configured with the necessary permissions.

1. Configure the `scripts/aws/config.sh` file with the necessary parameters;
2. Run `./scripts/aws/create-image.sh` to create an AMI with the application;
3. Run `./scripts/aws/launch-deployment.sh` to deploy the application to AWS.

To remove the deployment, you can run `./scripts/aws/terminate-deployment.sh`.