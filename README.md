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

## Deploying microservices

In the third part, we will deploy the monolith and the microservice into connected docker containers, implement some microservice patterns and deploy to the Jelastic cloud.
