# Overlay Network Simulator - HW3 and Course Project

## Setup and Usage

### Setup
The following tools are needed to run the simulation:
- SBT
- Scala
- Java


### Configuration
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
To run simulator: `sbt clean compile run`

Running the simulator assumes that the config has been appropriately set according to the Configuration section above.

## Implementation Overview
A number of parts comprise the simulator.

### ChordNodeActor
The `ChordNodeActor` Akka Actor forms the backbone of the simulator, representing one node in a Chord DHT ring. Each 
`ChordNodeActor` maintains the following state about itself:
- `nodeId`: Hashed ID of the node
- `m`: Bit size of the hash function. Used to determine the size of the finger table. Passed in during initialization 
phase
- `predecessor`: Represents the current node's predecessor. Tuple of `(Int, ActorRef)`, where the `Int` is the predecessor 
node's ID, and `ActorRef` is a reference to the predecessor node's actor instance.
- `fingerTable`: The node's finger table
- `movies`: Contains the movies stored by this node. A mapping from the hashed movie title (Int) to the `Movie` object.

#### Chord Node Lifecycle
A chord node goes through a number of stages in its lifecycle.

##### Initialization & Joining the Ring
Before the node can join a Chord ring, it needs to be initialized with appropriate data.
After the simulator creates an instance of the `ChordNodeActor`, it sends an `InitSelfRequest` to the node with the 
following data:
- `nodeId`: `Int` representing the node's hashed ID
- `m`: `Int` representing the bit size of the hash function
- `refNode`: `Optional[ActorRef]` representing the node that this new node will use as a reference for joining the ring

Once it receives the `InitSelfRequest` message, the new node does two things:
- Initialize internal state like `nodeId` and finger table
- Join the ring using the `refNode`

To join the ring, we follow the same algorithm described in the Chord paper. In short:

- If a null `refNode` is passed, then the node assumes that it is the first node to join the ring, and initializes its 
finger table state such that all of the fingers point back to itself.

- If a non-null `refNode` is passed, then the regular algorithm is followed to initialize the finger table.
