# Streambased Take Home

## Outline

This exercise is intended to test the following strengths:

 * Requirements understanding
* System design
* Java development, code style and ecosystem

There is no time limit set for this task but we usually find a good solution can be implemented in around 3hrs. 
If this task is requiring more than 8hrs of investment it is probably headed in the wrong direction, please reach back 
out to the person that assigned the task to you before investing this amount of time.

## Requirements

This exercise has the following dependencies:

* A Java 17+ SDK
* Docker (+ docker-compose)

## The task

ShiftLeftNet has an observability problem. As a modern, data driven company they are extensive users of Kafka in their 
operational architecture. Their developers create microservices that produce and consume messages from a managed Kafka 
service, and it works great.

Unfortunately ShiftLeftNet’s provider has a strange billing policy and this is causing them problems. The provider 
charges according to the number of messages stored on their servers at any given moment, for instance if the provider 
charges 1p per message and a producer produces 100 messages it will cost £1.

Recently ShiftLeftNet has had to pay some huge bills under this scheme but they cannot tell if these amounts are 
correct or not as they have no way of knowing how many messages are stored server side at any given time.

Your mission is a simple one: Develop a tool that can tell the number of messages stored server side on a topic.

This tool should handle ShiftLeftNet’s 3 different type of producing microservices:

* Basic producers - services that append messages to the end of the topic in the usual Kafka fashion
* Transactional producers - services that produce messages in transactions (https://www.confluent.io/en-gb/blog/transactions-apache-kafka/) that can either be committed or aborted. The tool should show the total number of messages on the topic as well as the number of messages that belong to aborted transactions.
* Comparing producer - services that use Kafka’s compaction feature (https://developer.confluent.io/courses/architecture/compaction). The tool should show the total number of messages stored taking into account that compaction may have taken place.

## Environment

The task physical environment consists of:

* A Zookeeper container
* A single node Kafka container
* A "microservices-simulator" container

These should run continuously for the life of the exercise

* A "solution" container is also provided that will execute your solution. This should not run continuously and instead should complete, print the information required and exit. It should be possible to execute this container multiple times.

The logical environment for this task consists of 3 Kafka topics, the data stored in these topics is irrelevant to the task, only message counts matter:

* basic - a simple topic being produced to as per textbook Kafka
* transactional - a topic being written to with transactions. Some transactions are committed and some are aborted.
* compacting - a topic using Kafka's builtin log compaction. Older messages with duplicate keys will be "compacted away".

## How to complete this task

A sample maven project has been created for you in the test repo. Please add your code in this project.

To build the project please run:

```
mvn clean package
```

After this you can start the environment with:

```
docker-compose up -d
```

After this please add your code to the project in the `YourSolution` class. Feel free to add any further classes etc. 
as required.

To verify your project please run:

```
docker-compose up solution
```

Your project should print any output to stdout and there are no restrictions in how the information is displayed.

## Restrictions

* You should not introduce further infrastructure dependencies into the project (e.g. postgres etc.) You may however, add any maven libraries you require. If you need to persist data you may persist to Kafka (https://github.com/rayokota/kcache is a great library for this)
* Please do not modify the docker environment. All resources and configurations provided should be sufficient to complete the task.

## Some tips

* Your solution should include unit tests, however these only need to be representative of your testing approach. We will not assign credit for full test coverage.
* Integration tests can be ignored for this project
* The resources used by this task have been sized to make sure that it can run locally on a laptop. Optimal solutions should consider much higher message volumes, distributed execution and resiliency concerns.
* The kafka1 container includes all the Kafka console tools for debugging
