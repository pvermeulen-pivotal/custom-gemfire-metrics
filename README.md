# Custom GemFire Metrics

This project is an example of custom metrics for both GemFire clients and servers.
Custom GemFire metrics provides an example on how to create custom application metrics and expose these
metrics using a custom metrics publisher. The customer metrics publisher is automatically started and stopped when a GemFire client
or server is started or stopped.

### Client Implementation
To run the client application execute the `runClient.sh` script.

The client example starts a GemFire client application that creates four (4) metrics:

* Message received count
* Message produced count
* Message processing time
* Message queue gauge

After the metrics have been created and populated, the client application will wait until the user enter
"c" and presses enter to continue. While waiting for user input, use the URL 
`http://localhost:9114/application-metrics` to display metric values. Once
you have viewed the metrics stop the application by pressing "c" and enter.

### Server Implementation

The server example uses a GemFire function that creates four (4) metrics:

* Message received count
* Message produced count
* Message processing time
* Message queue gauge

Execute the function below to start the gathering of custom server metrics.

`execute function --id=ApplicationServerMetricsFunction --member {a member name} --arguments="test_server,start"`

After a successful return from the function, use URL `http://localhost:9115/application-metrics` to display the custom metrics.

To stop the Custom Metrics on the server, execute the function below.

`execute function --id=ApplicationServerMetricsFunction --member {a member name} --arguments="test_server,end"`

### Sample Metrics

To see a sample output of both the client and server custom metrics, review the project files `client-metrics` and `server-metrics`.

The Custom metrics publishing service can also capture JVM and GemFire metrics if so desired. 

#### Client

To enable JVM and GemFire metrics, pass the property -Dcapture-jvm-gemfire-stats=true in the runClient.sh.

#### Server

When starting a server add the gemfire-metrics-1.0.0-SNAPSHOT.jar to the classpath.

To enable JVM and GemFire metrics, pass the property --J=-Dcapture-jvm-gemfire-stats=true when starting a GemFire server.

`gfsh> start server --name server --dir server --locators localhost[10334] --classpath target/gemfire-metrics-1.0.0-SNAPSHOT.jar:jars/* --J=-Dcapture-jvm-gemfire-stats=false --J=-Dmetrics-port=9115`


### Note

To have GemFire client or server start abnd stop custom metrics publisher a
`META-INF.services` files needs to be created under resources and must contain `org.apache.geode.metrics.MetricsPublishingService`.

