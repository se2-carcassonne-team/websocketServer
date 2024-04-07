# WebSocket Server

This repository contains the implementation for WebSocket communication and database storage in Java Spring Boot. The implementation utilizes a broker with tbe STOMP protocol for communication and an in-memory H2 database for data storage.

## Component-Based Layered Approach

Spring strongly encourages and supports the use of a component based approach/architecture which we try to properly implement to leverage its benefits. 

Every Spring Framework can be thought about in the layers:

- Presentation (take data using service layer and expose it the client via "Controllers" [here](./src/main/java/at/aau/serg/websocketdemoserver/controller))
- Service (use all the functionality exposed by persistence layer via "Services" [here](./src/main/java/at/aau/serg/websocketdemoserver/service))
- Persistence (handle interactions with database, via "Entities" and "Repositories" [here](./src/main/java/at/aau/serg/websocketdemoserver/domain/entity))

Since the Presentation Layer should have no knowledge of the Persistence Layer, we enforce a strict separation of the two layers by using DTOs and Entities. The Presentation Layer end-points only send and receive DTOs (Data Transfer Objects), while all layers below only work with Entity objects supplied by the persistence layer. The DTOs are defined in the `at/aau/serg/websocketdemoserver/domain/dto` package [here](./src/main/java/at/aau/serg/websocketdemoserver/domain/dto). 

Using separate objects in the different layers creates the necessity to convert between DTO and Entity objects. This is handled by mappers, see: [here](./src/main/java/at/aau/serg/websocketdemoserver/mapper).


## Broker Implementation with STOMP Protocol

The `at/aau/serg/websocketdemoserver/websocketConfig/WebSocketBrokerConfig.java` file contains the Configuration of the WebSocket broker with the STOMP protocol. STOMP (Simple Text Oriented Messaging Protocol) is a lightweight messaging protocol that defines the format and rules for data exchange. An important reason for choosing WebSocket + STOMP is that this protocol combination allows us to easily push information from the server to multiple clients over connections that are kept alive, which is essential in an online multiplayer game.

The end-points and their implementation are contained in the `at/aau/serg/websocketdemoserver/controller` package [here](./src/main/java/at/aau/serg/websocketdemoserver/controller).

## H2 Database

H2 is a lightweight database that is well integrated into Spring Boot. It runs in-memory while the server application is running and does not use any persistent storage. Since we do not need to persistently save data in the scope of our app, this is not a drawback. The big benefits of using H2: it runs from our Springboot application and we do not need to care about setting up our own separate database, we can setup everything using Java (only one language for code-base), H2 makes it very easy to read and write to the database using Entity Objects.

The tables of the database are defined by the Entities: [here](./src/main/java/at/aau/serg/websocketdemoserver/domain/entity).

The interactions with the database are defined by the Repositories: [here](./src/main/java/at/aau/serg/websocketdemoserver/domain/entity/repository).

## Testing:

To achieve good test-coverage we use unit tests for the repository methods (to check if the correct database operations are executed), see: [here](./src/test/java/at/aau/serg/websocketdemoserver/repository). And integration tests for the controller methods / end-points (to check if client requests result in the proper responses and database changes), see: [here](./src/test/java/at/aau/serg/websocketdemoserver/controller).


