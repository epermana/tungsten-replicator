# Introduction #

Tungsten Replicator has an API, which can be accessed via JMX (convenient if using JAVA) or JSON (convenient for any client which can talk via a REST-like protocol). JSON interface is exposed via HTTP or HTTPS protocol, running in the Replicator itself.

# Details #

Replicator uses [Jolokia](http://www.jolokia.org) to front-end JSON interface. Minimal configuration is needed to enable HTTP access, a bit more - to enable secure HTTPS.

# Enabling JSON #

## JSON over HTTP ##

  1. Open `tungsten-replicator/conf/wrapper.conf` configuration file;
  1. Find a line similar to the following:
> > `#wrapper.java.additional.4=-javaagent:../../tungsten-replicator/lib/jolokia-jvm-jdk6-<VERSION>-agent.jar=port=19999,host=localhost,user=tungsten,password=secret`
  1. Uncomment it by removing "#" at the beginning, which will enable:
    * Plain HTTP protocol, if only `port` and `host` options are specified;
    * HTTP Authentication protocol, if `user` and `password` are specified too.
  1. Restart Replicator to enable changes.

Description of the options:

  * `port=19999` - port HTTP server is listening on;
  * `host=localhost` - host HTTP server is listening on;
  * `user` and `password` - HTTP Authentication credentials.

## JSON over HTTPS _(recommended)_ ##

Opening up JSON interface allows full control over the Replicator, thus security measures must be implemented.

In order to enable secure HTTPS protocol follow the same steps as in the chapter above, but, additionally, append and customize the following options:

`protocol=https,keystore=/opt/keystore.jks,keystorePassword=secret`

A complete example:

`wrapper.java.additional.4=-javaagent:../../tungsten-replicator/lib/jolokia-jvm-jdk6-<VERSION>-agent.jar=port=19999,host=localhost,user=tungsten,password=secret,protocol=https,keystore=/opt/keystore.jks,keystorePassword=secret`

Description of the options:

  * `protocol=https` - enables HTTPS processing;
  * `keystore=/path/to/keystore` - must point to an existig keystore, holding the server side certificate. Eg. of how to generate a self-signed certificate:

> `keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048`
  * `keystorePassword=password` - a password used during generation of the keystore.

## Usage ##

Below is a list of examples that map common `trepctl` calls to JSON HTTP requests.

Calls to JMX methods that accept simple arguments (eg. int, String) can be encoded via URL GET request; for more complex methods (eg. which take Map as an argument) POST request is utilized. Command line `curl` tool is used for the following illustration, as it supports both.

  * `trepctl -service SERVICE_NAME online`
> `curl "http://tungsten:secret@localhost:19999/jolokia/exec/com.continuent.tungsten.replicator.management.OpenReplicatorManager:type=SERVICE_NAME/online"`
  * `trepctl -service SERVICE_NAME offline`
> `curl "http://tungsten:secret@localhost:19999/jolokia/exec/com.continuent.tungsten.replicator.management.OpenReplicatorManager:type=SERVICE_NAME/offline"`
  * `trepctl -host HOST_NAME -service SERVICE_NAME setrole -role master`
> `curl "http://tungsten:secret@HOST_NAME:19999/jolokia/exec/com.continuent.tungsten.replicator.management.OpenReplicatorManager:type=default/setRole/master/thl:/--/MASTER_HOST:2112"`
  * `trepctl -host HOST_NAME -service SERVICE_NAME setrole -role slave -uri thl://MASTER_HOST:2112`
> `curl "http://tungsten:secret@HOST_NAME:19999/jolokia/exec/com.continuent.tungsten.replicator.management.OpenReplicatorManager:type=default/setRole/slave/thl:/--/master_host:2112"`
  * `trepctl -service SERVICE_NAME  status -names tasks`
> `curl "http://tungsten:secret@localhost:19999/jolokia/exec/com.continuent.tungsten.replicator.management.OpenReplicatorManager:type=SERVICE_NAME/statusList/tasks"`
  * `trepctl -service SERVICE_NAME status -names shards`
> `curl "http://tungsten:secret@localhost:19999/jolokia/exec/com.continuent.tungsten.replicator.management.OpenReplicatorManager:type=SERVICE_NAME/statusList/shards"`
  * `trepctl -service SERVICE_NAME  status -names stores`
> `curl "http://tungsten:secret@localhost:19999/jolokia/exec/com.continuent.tungsten.replicator.management.OpenReplicatorManager:type=SERVICE_NAME/statusList/stores"`
  * `trepctl -service SERVICE_NAME  online -seqno SEQNO`
> `curl -d '{"type":"EXEC","mbean":"com.continuent.tungsten.replicator.management.OpenReplicatorManager:type=SERVICE_NAME","operation":"online2","arguments":[{"toSeqno":SEQNO}]}' http://tungsten:secret@localhost:19999/jolokia/`
  * `trepctl -service SERVICE_NAME  online -event EVENT`
> `curl -d '{"type":"EXEC","mbean":"com.continuent.tungsten.replicator.management.OpenReplicatorManager:type=SERVICE_NAME","operation":"online2","arguments":[{"toEventId":"EVENT"}]}' http://tungsten:secret@localhost:19999/jolokia/`
  * `trepctl -service SERVICE_NAME  offline-deferred -seqno SEQNO`
> `curl -d '{"type":"EXEC","mbean":"com.continuent.tungsten.replicator.management.OpenReplicatorManager:type=SERVICE_NAME","operation":"offlineDeferred","arguments":[{"atSeqno":SEQNO}]}' http://tungsten:secret@localhost:19999/jolokia/`
  * `trepctl -service SERVICE_NAME  wait -state STATE -limit SECONDS`
> `curl "http://tungsten:secret@localhost:19999/jolokia/exec/com.continuent.tungsten.replicator.management.OpenReplicatorManager:type=SERVICE_NAME/waitForState/STATE/SECONDS"`
  * `trepctl -service SERVICE_NAME  wait -applied N -limit SECONDS`
> `curl "http://tungsten:secret@localhost:19999/jolokia/exec/com.continuent.tungsten.replicator.management.OpenReplicatorManager:type=default/waitForAppliedSequenceNumber/N/SECONDS"`

Notes:
  1. "`/--/`" - is an escape sequence for a double slash (i.e. “//”).
  1. HOST\_NAME, MASTER\_HOST, SERVICE\_NAME, SEQNO, EVENT, STATE, N, SECONDS  are variables to be replaced with corresponding values.
  1. Remote Replicators can be controlled by changing the “@localhost” part of the URL to the remote host name. However, Jolokia must be configured to listen on this public host name rather than the localhost (see the "`host=`" option in `wrapper.conf` above).

For more operations see `com.continuent.tungsten.replicator.management.OpenReplicatorManagerMBean` and `com.continuent.tungsten.replicator.management.ReplicationServiceManagerMBean` interfaces in the code line.