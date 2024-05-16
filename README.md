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
    * [`aws`](scripts/aws) - Scripts to deploy the application to AWS;
    * [`instrumentation`](scripts/instrumentation) - Scripts to instrument the application.

## Usage

### Requirements

* Java 11+
* Maven 3.6.3+

### Running the Application

1. Make sure your `JAVA_HOME` environment variable is set to Java 11+ distribution
2. Run `mvn clean package` in the root directory to build the application
3. Run `java -jar app/webserver/target/webserver-1.0-SNAPSHOT-jar-with-dependencies.jar` to start the web server

To run a workload directly, you can use the following commands:

* Ray
  Tracing: `java -cp app/target/raytracer-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.raytracer.WebServer <input-scene-file.txt> <output-file.bmp> 400 300 400 300 0 0 [-tm=<texmap-file.bmp>] [-aa]`
* Blur
  Image: `java -cp app/target/imageproc-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler <input-file> <output-file>`
* Enhance
  Image: `java -cp app/target/imageproc-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler <input-file> <output-file>`
