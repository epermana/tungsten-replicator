# Introduction #

Tungsten Replicator is an open source high performance replication engine that can replace MySQL native replication and greatly enhance the user experience with a broader feature set.


# Overview #

Tungsten Replicator is not integrated into MySQL. It is, in fact, written in Java, and it is thus totally external to the database server. The little inconvenience of this non-integration is more than compensated by the set of features that the replicator offers.
MySQL native replication is a simple pipeline of data from one master to one or more slaves. This simple paradigm makes MySQL replication easy to deploy and administer, but it also results in a lack of features and functionality.
Most of what MySQL native replication cannot do, Tungsten Replicator can achieve easily with its dynamic architecture.
Just to mention a few issues, Tungsten Replicator implements **Global Transaction IDs**, which make master **seamless failover** an easy task. MySQL masters know little or nothing about its slaves, and the slaves themselves have no visibility of the other nodes. Tungsten replicator allows the DBA to see the replication cluster as a whole, rather than as a collection of servers. Operations like master switch can be performed from any node, and quite easily.
More notable features are:
  * Parallel replication;
  * Multiple source replication;
  * Bi-directional replication;
  * Filters.