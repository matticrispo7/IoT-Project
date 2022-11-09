# Connected City
This is a master's degree project of Distributed Systems developed in 2022. <br />

## Description
The project is related to the traffic management in a smart city crossed by a river. The *Californium* Java framework has been used.
For detailed information, read the documentation.

## Architecture
The developed system is based on a multi thread architecture with 3 main components: a single server, several clients (*vehicle*, *police* and *citizen*) and observable resources (*traffic light*).<br />
Initially, the server registers the resources giving them a unique URI with the following structure:  tf*x_y* where *x* is the i-th bridge and *y*refers to the side of the bridge on which the traffic light is placed.<br />
The server, via HTTP PUT requests, updates the state of the resources every T seconds, swapping them.<br />
To simulate a real scenario, the single client *vehicle* generates M threads which, thanks to the naming service offered by the server, randomly obtain a URI of a resource to connect with. <br/> <br/>
In a first step, the client thread sends a HTTP POST request with it's ID to the resource and, depending on the status of it, either gets stuck or has a chance to cross the bridge; again, to simulate a real-world scenario in which vehicles may take a different amount of time to cross the bridge (i.e., the traffic flow on the bridge does not remain constant), the resource draws a random number of IDs from its queue and communicates these to the vehicles.

<br />
Through HTTP requests, the *police* client has the ability to obtain information about the total number of vehicles that have passed over the bridges and how many of them crossed with red light.<br />
Finally, the *citizen* client, taking advantage of the same naming service offered by the server, receives the URI of the resource it wants to connect to and gets information about its status.
