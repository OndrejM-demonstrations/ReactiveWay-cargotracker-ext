Reactive improvements of Cargo Tracker
================================

This project provides an example how to enhance an existing Java EE application 
written in a traditional approach in a reactive way to improve its responsiveness.

The project consistes of 2 repositories:
 - [ReactiveWay-cargotracker](https://github.com/OndrejM-demonstrations/ReactiveWay-cargotracker) - updates to the original CargoTracker application
 - [this repository](https://github.com/OndrejM-demonstrations/ReactiveWay-cargotracker-ext) - additional modules required by the updated CargoTracker application, and information about all the changes and how to run the application
 
# Instructions for the hands on lab

See the instructions to download the recommended tools and setup your environment in this [README](workshop-dependencies/README.adoc) file.

## Improve the monolithic application

In the first part, we will improve the monolithic application. The steps are described in the [ReactiveWay-cargotracker](https://github.com/OndrejM-demonstrations/ReactiveWay-cargotracker/blob/devoxx-uk-2017/README.adoc) repository.

## Introduce a microservice
In the second part, we will separate a module of the monolith into a sandalone microservice, running with Payara Micro.

Branch `11_separate_microservice` in both repositories.

2 new maven modules:
 - Pathfinder service (WAR) - a separate microservice providing GraphTraversalService service as both a REST resource and via Payara CDI event bus
 - Pathfinder API (JAR) - common code reused in both the monolithic application and the Pathfinder micro service
 
To run the demo:
 1. run `mvn clean install` in the root of this repository (for the top-level maven module)
 2. deploy the monolithic cargo-tracker application to Payara Server as before
 3. run Pathfinder micro service with Payara Micro - go to the directory `pathfinder/target` and execute: `java -jar payara-micro.jar --autobindhttp --deploy pathfinder.war`

The `--autobindhttp` argument to Payara Micro instructs the service to bind the HTTP listener to an available port. Since the monolithic application already occupies the port 8080, therefore the Pathfinder service will probably bind to the port 8081. We can find out the port from the console output. We can check that the application is running with the following URL: [http://localhost:8081/pathfinder/rest/graph-traversal/shortest-path?origin=CNHKG&destination=AUMEL](http://localhost:8081/pathfinder/rest/graph-traversal/shortest-path?origin=CNHKG&destination=AUMEL)

The port number is not important and can even vary. The monolith communicates with the service using the CDI even bus messages and doesn't use the REST endpoint. 


## Deploying microservices

In the third part, we will deploy the monolith and the microservice into connected docker containers, implement some microservice patterns and deploy to the Jelastic cloud.
