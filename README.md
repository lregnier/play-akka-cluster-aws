# Akka Cluster with Play in AWS!

## Running the cluster locally
For running the first node just execute below command:

```sh
sbt run
```

The app will be started in default HTTP (9000) and Akka (2551) ports.

For running a second, or consecutive nodes, override default HTTP and Akka ports doing as follows:

```sh
sbt "run 9001" -Dakka.port=2552
```

## Running the cluster in AWS
Make sure below option is on, the region matches and configured Akka port is open.

## Poking the API

List all nodes in the cluster:

```
GET /cluster/nodes
```

Singleton counter among all nodes in the cluster:
```
POST /cluster/singleton/count
```
