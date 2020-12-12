# Overlay Network Simulator - HW3 and Course Project

## Setup and Usage

### Setup
The following tools are needed to run the simulation:
- SBT
- Scala
- Java
- Docker
- `docker-compose`

The Docker image for the simulator can be found here: https://hub.docker.com/r/mrusna4/cs441-course-project
The video demo of the AWS deployment can be found here: https://www.youtube.com/watch?v=WTCnJ-O168U

### Configuration
The setting for running a Can or Chord simulation is in `.env`
The configuration for the simulator is located in the `resources/application.conf` file.
The following information is available in the config:

|Key|Data Type|Description|
|---|----|----|
|`cs441.OverlayNetwork.m`|Int|Number of bits used for the hash values. Used to determine the size of the finger table.|
|`cs441.OverlayNetwork.defaultTimeout`|Int|Timeout (in seconds) used in Akka actor calls.|
|`cs441.OverlayNetwork.snapshotBasePath`|String|Directory (without trailing slash) where the global state snapshot files will be stored.|
|`cs441.OverlayNetwork.network.nodes`|List[Int]|List of integers representing the IDs of the nodes to create|
|`cs441.OverlayNetwork.data`|List[Movie]|List of objects containing the movie data. Required data for each movie is `title`, `year`, `revenue`|

### Usage
To run tests: `sbt clean compile test`

To run simulator: 
- Set the `TYPE` environment variable in `.env` file to be either CAN or CHORD
- Run `docker-compose up`

Running the simulator assumes that the config has been appropriately set according to the Configuration section above.
Once started, the simulation will run and the movies, started in the `cs441.OverlayNetwork.data` config will be written 
and then read from the network.

Once finished, Ctrl-C will end the simulator and `docker-compose down` will clean up the containers.

The final output of the simulator will be a list of logged statistics. This will include the recorded time to execute each
operation, as well as the average time for a read and a write

This is an example output for a Chord simulation:
````Java
app_0   | 05:41:12.235 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_WRITE_MOVIE, data = 300, unit = ms)
app_0   | 05:41:12.235 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_READ_MOVIE, data = 68, unit = ms)
app_0   | 05:41:12.235 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_WRITE_MOVIE, data = 6, unit = ms)
app_0   | 05:41:12.235 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_READ_MOVIE, data = 30, unit = ms)
app_0   | 05:41:12.235 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_WRITE_MOVIE, data = 10, unit = ms)
app_0   | 05:41:12.235 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_READ_MOVIE, data = 33, unit = ms)
app_0   | 05:41:12.235 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_WRITE_MOVIE, data = 13, unit = ms)
app_0   | 05:41:12.235 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_READ_MOVIE, data = 34, unit = ms)
app_0   | 05:41:12.235 [main] INFO akka.actor.typed.ActorSystem - CHORD Avg Write Time: 82 ms
app_0   | 05:41:12.236 [main] INFO akka.actor.typed.ActorSystem - CHORD Avg Read Time: 41 ms
````

This is an example output for a CAN Simulation:
```Java
app_0   | 05:53:40.747 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_WRITE_MOVIE, data = 351, unit = ms)
app_0   | 05:53:40.747 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_READ_MOVIE, data = 29, unit = ms)
app_0   | 05:53:40.747 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_WRITE_MOVIE, data = 11, unit = ms)
app_0   | 05:53:40.747 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_READ_MOVIE, data = 9, unit = ms)
app_0   | 05:53:40.747 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_WRITE_MOVIE, data = 13, unit = ms)
app_0   | 05:53:40.747 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_READ_MOVIE, data = 17, unit = ms)
app_0   | 05:53:40.747 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_WRITE_MOVIE, data = 6, unit = ms)
app_0   | 05:53:40.747 [main] INFO akka.actor.typed.ActorSystem - RuntimeStatistic (category = CHORD_READ_MOVIE, data = 7, unit = ms)
app_0   | 05:53:40.748 [main] INFO akka.actor.typed.ActorSystem - CHORD Avg Write Time: 95 ms
app_0   | 05:53:40.748 [main] INFO akka.actor.typed.ActorSystem - CHORD Avg Read Time: 15 ms
```

## Implementation Overview
A number of parts comprise the simulator.

### CanNodeActor
The `CanNodeActor` Akka actor forms the backbones of the Can, representing one node in the Chord network.

### ChordNodeActor
The `ChordNodeActor` Akka Actor forms the backbone of the Chord simulator, representing one node in a Chord DHT ring. Each 
`ChordNodeActor` maintains the following state about itself:

* `nodeId`: Hashed ID of the node

* `m`: Bit size of the hash function. Used to determine the size of the finger table. Passed in during initialization 
phase

* `predecessor`: Represents the current node's predecessor. Tuple of `(Int, ActorRef)`, where the `Int` is the predecessor 
node's ID, and `ActorRef` is a reference to the predecessor node's actor instance.

* `fingerTable`: The node's finger table

* `movies`: Contains the movies stored by this node. A mapping from the hashed movie title (Int) to the `Movie` object.

#### Chord Node Lifecycle
A chord node goes through a number of stages in its lifecycle.

##### Initialization & Joining the Ring
Before the node can join a Chord ring, it needs to be initialized with appropriate data.
After the simulator creates an instance of the `ChordNodeActor`, it sends an `InitSelfRequest` to the node with the 
following data:

* `nodeId`: `Int` representing the node's hashed ID

* `m`: `Int` representing the bit size of the hash function

* `refNode`: `Optional[ActorRef]` representing the node that this new node will use as a reference for joining the ring

Once it receives the `InitSelfRequest` message, the new node does two things:

* Initialize internal state like `nodeId` and finger table

* Join the ring using the `refNode`

To join the ring, we follow the same algorithm described in the Chord paper. In short:

* If a null `refNode` is passed, then the node assumes that it is the first node to join the ring, and initializes its 
finger table state such that all of the fingers point back to itself.

* If a non-null `refNode` is passed, then the regular algorithm is followed to initialize the finger table.

#### Using the Can/Chord Nodes to Read/Write Data

##### ApiServer
A Akka/HTTP server exposing our Can/Chord network. The API exposes 1 endpoint, `movie` and accepts `GET` and `POST` requests. A 
`POST` request will write a movie to the network, and a `GET` will read a movie. 

##### UserActor
An Akka Actor representing an end user consuming this movie API we created. 

`UserActor`s accept two types of commands: 
    - `ReadMovie(movieTitle: Int)`
    - `WriteMovie(hashedMovieTitle: Int, movie: Movie)`

##### Adding Movies
When we want to add a movie, we can send a `WriteMovieRequest` to one of the nodes in the ring. By default, we 
use Node 0. In `Homework3.scala`, there is a `makeWriteRequest(movie: Movie)` wrapper method we can call to easily make 
the RPC calls for us.

Once the node receives the request, it makes a call to an internal method, `lookupNode(nodeId: Int)`, to determine which 
node is responsible for this movie. Then, depending on the nodeId, we do one of the following:

* If the node is **equal** to the current node, then we simply create a new mapping from the hashed movie title to the 
movie object in the `movies` map.

* **Otherwise**, send a `WriteMovieRequest` to the node that is responsible for the file

Then, the result from `WriteMovieRequest` is a boolean (successful/unsuccessful), which we send back to the caller.

##### Finding Movies
The process for finding movies is nearly identical to inserting them. The analogous request for this operation is 
`ReadMovieRequest`, and the corresponding wrapper method in `Homework3.scala` is 
`makeReadRequest(movieTitleHash: Int): Movie`. 

Once the node receives the request, it makes a call to an internal method, `lookupNode(nodeId: Int)`, to determine which 
node is responsible for this movie. Then, depending on the nodeId, we do one of the following:

* If the node is **equal** to the current node, then we send back the corresponding movie from the `movies` map.

* **Otherwise**, send a `ReadMovieRequest` to the node that is responsible for the file.

Then, the result from `ReadMovieRequest` is a `Movie` object. In the event that the requested movie could not be found,
we hand back a "not found" object, which contains the following data:
`Movie(title = "Movie_Not_Found", year = 0, revenue = 0.0)` 