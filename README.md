Reactive improvements of Cargo Tracker
================================

This project provides an example how to enhance an existing Java EE application 
written in a traditional approach in a reactive way to improve its responsiveness.

The project consists of 2 repositories:
 - [ReactiveWay-cargotracker](https://github.com/OndrejM-demonstrations/ReactiveWay-cargotracker) - updates to the original CargoTracker application
 - [this repository](https://github.com/OndrejM-demonstrations/ReactiveWay-cargotracker-ext) - additional modules required by the updated CargoTracker application, and information about all the changes and how to run the application
 
# Instructions for the hands on lab

## Instructions to setup the lab

If you want to setup the the lab resources on your operating system:
 
1. See the instructions to download the recommended tools and setup your environment in this [README](workshop-dependencies/README.adoc) file
2. Download Netbeans IDE with preinstalled required plugins either from [this location (US region)](https://s3-us-west-1.amazonaws.com/holjavaoneus/Workspace/netbeans.zip) or [this location (EU region)](https://s3-eu-west-1.amazonaws.com/holjavaone/Workspace/netbeans.zip)
3. It's also a good idea to download the workspace from either [this location (US region)](https://s3-us-west-1.amazonaws.com/holjavaoneus/Workspace/workspace.zip) or [this location (EU region)](https://s3-eu-west-1.amazonaws.com/holjavaone/Workspace/workspace.zip) - the workspace includes template and working source code projects and Payara Server installations in one place, as they are preinstalled on the alternative virtual machine as described in the [Instructions for Virtual Machine](instructions-for-virtual-machine.adoc). But it's not necessary as these should be already installed by following the step on to setup your environment

If you want to run the lab inside a virtual machine in isolation to your operating system:

1. Install the latest version of VirtualBox ([https://www.virtualbox.org/](https://www.virtualbox.org/)) on your operating system
2. Download the virtual machine image from [https://s3-eu-west-1.amazonaws.com/holjavaone/VirtualBox/HOL1320.ova](https://s3-eu-west-1.amazonaws.com/holjavaone/VirtualBox/HOL1320.ova) (5GB)
3. Optionally download the MD5 checksum for the image from [https://s3-eu-west-1.amazonaws.com/holjavaone/VirtualBox/HOL1320.ova.md5](https://s3-eu-west-1.amazonaws.com/holjavaone/VirtualBox/HOL1320.ova.md5) and check that the image was downloaded correctly
4. Import the downloaded OVA image into VirtualBox

Also check out [Lab instructions for Virtual Machine](instructions-for-virtual-machine.adoc) which describe the lab resources preinstalled in the virtual machine.

## Improve the monolithic application

In the first part, we will improve the monolithic application. The steps are described in the [ReactiveWay-cargotracker](https://github.com/OndrejM-demonstrations/ReactiveWay-cargotracker/blob/javaone2017/README.adoc) repository.

## Introduce reactive microservices architecture

In the second part, we will separate a module of the monolith into a standalone microservice, running with Payara Micro. We will then look at the ways how to extend the reactive concepts to the architecture of microservices, beyond a single monolith.

The starting point is the branch `10_monolith_before_splitting`, which already contains the previous reacive improvements in the original monolithic application.


### Introduce a microservice

The branch `11_separate_microservice` in both repositories.

2 new maven modules:
 - Pathfinder service (WAR) - a separate microservice providing GraphTraversalService service as both a REST resource and via Payara CDI event bus
 - Pathfinder API (JAR) - common code reused in both the monolithic application and the Pathfinder micro service
 
To run the demo:
 1. run `mvn clean install` in the root of this repository (for the top-level maven module)
 2. deploy the monolithic cargo-tracker application to Payara Server as before
 3. run Pathfinder micro service with Payara Micro - go to the directory `pathfinder/target` and execute: `java -jar payara-micro.jar --autobindhttp --deploy pathfinder.war`

The `--autobindhttp` argument to Payara Micro instructs the service to bind the HTTP listener to an available port. Since the monolithic application already occupies the port 8080, therefore the Pathfinder service will probably bind to the port 8081. We can find out the port from the console output. We can check that the application is running with the following URL: [http://localhost:8081/pathfinder/rest/graph-traversal/shortest-path?origin=CNHKG&destination=AUMEL](http://localhost:8081/pathfinder/rest/graph-traversal/shortest-path?origin=CNHKG&destination=AUMEL)

The port number is not important and can even vary. The monolith communicates with the service using the CDI even bus messages and doesn't use the REST endpoint. 

### Decouple microservices

In this step, the microservices share the API code. To enable that the service API can evolve without redeploying its clients, we need to avoid the shared code. 

This is done in the branch `11_separate_microservice_02_decoupled_api` in both repositories.

Since the API consists of serializable class, we can decouple the API by copying the API classes into the client so that they are still available in both services, but maitained separately. We need to ensure that the `serialVersionUID` remains equal and that the future contract changes are compatible with the standard serialization mechanism, or introduce a custom serialization.

### Introduce JCache for caching and process synchronization

The branch `12_load_balancing_01_jcache` introduces JCache API (JSR 107). 

JCache can be used for caching of results to optimize repetitive processing. But if the cache is distributed, it also provides distributed locks, which we will use to synchronize message observers so that at most one of them processes the message.

### Run the microservices

Deploy and run the main application in a usual way on Payara Server.

Run the Pathfinder microservice with Payara Micro:

`java -jar payara-micro.jar --deploy pathfinder.war --autobindhttp`

Try if the route cargo page is working, check the logs of the main application and the Pathfinder service.

Build a standalone executable JAR with the Pathfinder microservice:

`java -jar payara-micro.jar --deploy pathfinder.war --autobindhttp --outputuberjar pathfinder-standalone.jar`

Run a second instance of the Pathfinder microservice, now using the standalone JAR:

`java -jar pathfinder-standalone.jar`

Request the route cargo page in 2 or more different windows at the same time and observer that the requests are load-balanced to one or the other Pathfinder instance.

## Deploying microservices with Docker

In the third part, we will deploy the monolith and the microservice into connected docker containers, implement some microservice patterns and deploy to the Jelastic cloud.

### Build Docker Image of Payara Server with additional configuration

We need to enable Hazelcast in Payara Server, therefore we'll build a custom Payara Server image. The instructions to use the stock payara/server-full image and its Dockerfile can be found in the [Docker Hub](https://hub.docker.com/r/payara/server-full/).

Checkout the branch `13_deploy_to_docker_01_simple` in both repositories.

Go into the `cargo-tracker/docker` directory and run the following command:

`docker build -t reactivems/payara-server .`

### Running the main application in Docker

Leave the `13_deploy_to_docker_01_simple` branch checked out.

Rebuild the cargo-tracker main application with `mvn install`.

Run the application inside Docker with the following command, with PATH_TO_THE_GITHUB_REPO substituted by the path to parent folder of the cargo-tracker project:

```
docker run -p 8080:8080 -v 'PATH_TO_THE_GITHUB_REPO/cargo-tracker/target/autodeploy':/opt/payara41/glassfish/domains/domain1/autodeploy reactivems/payara-server bin/asadmin start-domain -v
 ```

Test that the application is running at the URL: [localhost:8080/cargo-tracker](http://localhost:8080/cargo-tracker/)

If the application isn't running, try building the application again to deploy it.

### Running the Pathfinder service in Docker

Run the application inside Docker with the following command, with PATH_TO_THE_GITHUB_REPO substituted by the path to parent folder of the pathfinder project:

```
docker run -p 8081:8080  -v 'PATH_TO_THE_GITHUB_REPO/pathfinder/target':/opt/payara/deployments payara/micro java -jar /opt/payara/payara-micro.jar --deploy /opt/payara/deployments/pathfinder.war
 ```

Test that the service is running and exposes a REST resource at the URL: [localhost:8081/pathfinder/rest/graph-traversal/shortest-path?origin=CNHKG&destination=AUMEL](http://localhost:8081/pathfinder/rest/graph-traversal/shortest-path?origin=CNHKG&destination=AUMEL)

### Running multiple Pathfinder services in Docker

Run additional services with the same docker command, but with modified port mapping. It's not necessary to map the HTTP port. Map the Hazelcast port to bigger ports (5902:

```
docker run -p 8081:8080  -v 'PATH_TO_THE_GITHUB_REPO/pathfinder/target':/opt/payara/deployments payara/micro java -jar /opt/payara/payara-micro.jar --deploy /opt/payara/deployments/pathfinder.war
 ```
