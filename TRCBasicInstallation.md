
Up to [the Tungsten Replicator Cookbook](TungstenReplicatorCookbook.md)

# Basic Installation #

## Get the replicator ##

All the recipes about installation require that you get Tungsten Replicator binaries. This recipe explains how to do it once for all.

  1. Determine which is the latest version. The latest stable version is the one featured on [Tungsten Replicator project page](http://code.google.com/p/tungsten-replicator/)
  1. If you want the latest release, which probably has more features, more bug fixes, and possibly more unknown bugs, you may get the very latest binaries [from the build server](http://s3.amazonaws.com/files.continuent.com/builds/nightly/tungsten-2.0-snapshots/index.html).
  1. Download the replicator
  1. Create a temporary directory. This is **not** the directory where you want to install Tungsten. You will use it only to launch the installation command.
  1. Expand the tarball
  1. Get inside the newly created directory
  1. We refer to this directory as the **staging directory**
  1. Now you can follow the directions from another recipe.

## Using the cookbook: choose topology and customize your installation ##

Starting with Tungsten Replicator 2.0.7, there is a **./cookbook** directory , containing a collection of recipes to install several replication topologies.

The following topologies are supported

  * master/slave (one master, many slaves)
  * fan-in (many masters, one slave)
  * all-masters (many masters, with slave services for all nodes)
  * star schema (many masters, interconnected through a hub)

**GETTING STARTED:**

  1. unpack the Tungsten tarball
  1. edit ./cookbook/COMMON\_NODES.sh file, with the list of your nodes
  1. If you are using a number of nodes other than 4, in the NODES\_xxxxxx file,
    * add as many nodes you are using
    * check the variables ALL\_NODES, MASTERS, and SLAVES, and make sure that they list existing nodes
    * if you are using less than 4 nodes, comment out the extra ones
  1. edit ./cookbook/USER\_VALUES.sh
  1. Run the command corresponding to the topology you want to install. The command must run from the directory above the cookbook, i.e the directory from where you can access ./tools/tungsten-replicator.
For example:
> > ./cookbook/install\_master\_slave
> > ./cookbook/install\_all\_masters
> > ./cookbook/install\_fan\_in
> > ./cookbook/install\_star

## Checking if a cluster meets the requirements ##

To check if a cluster meets the requirements without installing, after you have set the values for the cookbook (previous section), you can run the following:

```
   $  ./cookbook/validate_cluster
```
If no message is shown after the execution, your cluster is clear. If you want to see all the gory details of the validation, you can run:

```
   $   ./cookbook/install_master_slave
```

## Install a master / slave cluster ##

![https://lh5.googleusercontent.com/-jgCZbwwB4WE/TfC6Ksv2LiI/AAAAAAAABIQ/Ygpazw8EvEQ/s640/master_slave.png](https://lh5.googleusercontent.com/-jgCZbwwB4WE/TfC6Ksv2LiI/AAAAAAAABIQ/Ygpazw8EvEQ/s640/master_slave.png)

  1. make sure that all the hosts meet the [requirements](http://code.google.com/p/tungsten-replicator/wiki/InstallationPreRequisites)
  1. run ` $ ./cookbook/install_master_slave`


This command takes several defaults into account.
It assumes that MySQL is installed in `/var/lib/mysql/`, that the server uses port `3306`, and that the binary logs are in the data directory. We use these values because they are the ones used by RPM installers.
If you use different values, please see [TRCBasicInstallation#Install\_a\_master\_slave\_directory\_with\_customized\_parameters](TRCBasicInstallation#Install_a_master_slave_directory_with_customized_parameters.md).


## Install a master slave directory with customized parameters ##

If the defaults used in [TRCBasicInstallation#Install\_a\_master\_/\_slave\_cluster](TRCBasicInstallation#Install_a_master_/_slave_cluster.md) are not convenient, you can instruct the replicator to use paths and ports that describe your server.

You should edit ./cookbook/USER\_VALUES.sh and enter the desired values

## Install more than one Tungsten Replicator in one host ##

You may install more than one Tungsten Replicator in the same host.
One good reason for doing it may be because you want to test several servers at once, or because you have more MySQL servers in the same host and they need to replicate from different masters.
In the first case, you may want to have a look at [Tungsten sandbox](http://code.google.com/p/tungsten-toolbox/). But if you want to do that on your own, you need to make sure that there is no overlapping of three base elements in the replicator:

  * the **THL directory** (or the whole --home-directory), which is the place where the Transaction history logs are stored. This directory contains one directory for every service. It is important that you don't use two replicators with the same service name pointing at this directory.
  * The **THL port**. This is the port used by the replicator to send the transaction records.
  * The **RMI port**. This port is used to send JMX messages that the replicator uses for its own functioning.

For example, after installing Tungsten master/slave, I want to use an additional database with port 7102 and data directory somewhere else.

With the following command, I install a second replicator, using a different directory for the replicator, and pointing to the existing replicator

```
./tools/tungsten-installer \
  --master-slave \
  --cluster-hosts=127.0.0.1 \
  --master-host=HOST_NAME_OF_THE_MASTER \
  --master-thl-port=2112 \
  --datasource-port=7102 \
  --datasource-user=msandbox \
  --datasource-password=msandbox \
  --service-name=SAME_NAME_OF_THE_MASTER_SLAVE_SERVICE  \
  --home-directory=/tmp/some_place \
  --thl-port=4112 \
  --rmi-port=12000 \
  --start-and-report
```

Your replicator will end up in /tmp/some\_place
To check the status you may run

```
/tmp/some_place/tungsten/tungsten-replicator/bin/trepctl -port 12000 services
```

## Install a direct slave with parallel replication ##

![https://lh5.googleusercontent.com/-dhH-sPprwrw/TfC6L_F5RDI/AAAAAAAABIY/UwtVngp962Y/s720/slave_direct.png](https://lh5.googleusercontent.com/-dhH-sPprwrw/TfC6L_F5RDI/AAAAAAAABIY/UwtVngp962Y/s720/slave_direct.png)

Direct slave replication is a quick method to set replication in a slave, without installing a master replicator.
It works by establishing a connection with the master, fetching the binary logs locally, and creating a THL stream with that.
The additional effort of creating relay logs is then compensated by using parallel replication.

This installation assumes that the slave is not running native MySQL replication. For that case, see [TRCBasicInstallation#Taking\_over\_replication\_from\_a\_MySQL\_slave\_in\_direct\_mode](TRCBasicInstallation#Taking_over_replication_from_a_MySQL_slave_in_direct_mode.md).

```
  TUNGSTEN_HOME=/opt/continuent
  MASTER=m1.mynetwork.com
  SLAVE1=m2.mynetwork.com
  ./tools/tungsten-installer \
    --direct \
    --master-host=$MASTER \
    --slave-host=$SLAVE \
    --master-user=tungsten \
    --slave-user=tungsten \
    --master-password=secret \
    --slave-password=secret \
    --service-name=mydirect \
    --channels=10 \
    --home-directory=$TUNGSTEN_BASE \
    --svc-parallelization-type=disk \
    --start-and-report 
```

Notice that you need to pass connection credentials for both the master and the slave. As in regular master/slave replication, there are defaults, which you can override using the following options (defaults in brackets)

```
  --master-host
  --master-port [3306]
  --master-user
  --master-password
  --master-log-directory  [/var/lib/mysql]
  --master-log-pattern    [mysql-bin]
```

For more information, see [Adding parallel replication in a hurry](http://scale-out-blog.blogspot.com/2011/08/adding-parallel-replication-to-mysql-in.html).

## Taking over replication from a MySQL slave in direct mode ##

In the previous recipe, we have shown how to start replication from a MySQL master, without a replicator on the master side.
If the slave is already using MySQL native replication with the intended master, you can take over from the native replication stream simply adding to your installation command
```
--native-slave-takeover 
```

With this command, Tungsten stops the MySQL slave, gets the binlog file and position from the slave status, and starts replicating from that point.

When the replicator goes offline, it will send a "CHANGE MASTER TO" command to the slave to update log file and position, so that you can continue moving data either with Tungsten or with native replication.

**BEWARE:**  If you use this option on a MySQL slave that has invalid slave setting or out-of-date slave position it can cause completely inscrutable problems as the replicator may start at the wrong position in the MySQL binlog.  (Ask us how we know...)  You can correct by turning off slave replication completely or removing the --native-slave-takeover option from the installation.  You can also set to false in the static properties file of your replication service after installation.

## Take over from native MySQL replication in master/slave mode ##

In the ./cookbook directory, there is a **takeover.sh** script that will do all the work for you

## Install Tungsten master/slave replication in a sandbox ##

[Tungsten Sandbox](http://code.google.com/p/tungsten-toolbox/wiki/TungstenSandbox) is a dedicated script that allows you to install
multiple dataabse servers and multiple Tungsten Replicator services in a single host.

![https://lh4.googleusercontent.com/-rHIfADez_jI/TfC6JQSSP8I/AAAAAAAABIM/7TtIX9XNAWw/s800/master_slave_sandbox.png](https://lh4.googleusercontent.com/-rHIfADez_jI/TfC6JQSSP8I/AAAAAAAABIM/7TtIX9XNAWw/s800/master_slave_sandbox.png)

Please refer to [Tungsten-Sandbox instructions](http://code.google.com/p/tungsten-toolbox/wiki/TungstenSandbox) for more info.

## Chain two replication clusters ##

Let's say that you have two replication clusters, one installed using [the master slave recipe](http://code.google.com/p/tungsten-replicator/w/edit.do#Install_a_master_/_slave_cluster) and one using [Tungsten sandbox](http://code.google.com/p/tungsten-toolbox/).
You want the master of the Tungsten sandbox in your local host to become a slave of the master at m1.mynetwork.com.

![https://lh3.googleusercontent.com/-id11YtdENpY/TnZrvWcDHhI/AAAAAAAABMs/j4EFFqsKwTE/chaining_tungsten_clusters.png](https://lh3.googleusercontent.com/-id11YtdENpY/TnZrvWcDHhI/AAAAAAAABMs/j4EFFqsKwTE/chaining_tungsten_clusters.png)

Go to the home directory of the tungsten sandbox master, and run this command:
```
cd $HOME/tdb2/db1
./tungsten/tools/configure-service -C \
  --local-service-name=tsandbox \
  --thl-port=12111 \
  --role=slave \
  --service-type=remote \
  --master-thl-host=m1.mynetwork.com \
  --master-thl-port=2112 \
  --datasource=127_0_0_1 \
  --svc-start \
  dragon
```

Notes:
  * The thl-port is the one defined by default for Tungsten Sandbox. If your cluster master uses a different THL, it myst be provided here;
  * local-service-name is the name of the service used by the Tungsten Sandbox master.

After this command, the two clusters are chained. Whatever is inserted into m1.mynetwork.com will also be inserted into the Tungsten sandbox master, and from there it will go to its slaves.

## Modify one or more properties with the installer ##

Tungsten Replicator gets its configuration from a file called static-SERVICE\_NAME.properties, located $TUNGSTEN\_HOME/tungsten/tungsten-replicator/conf/.
This file can be edited with a regular text editor. If you know what you are doing, you can fine tune the replicator to your will.
The procedure is not painless. You need to install the replicator first, then edit the file, then eventually restart the replicator.

This procedure is difficult to script, and it is especially inconvenient when the changed property needs to be there right when the replicator starts.

To your help, there is an option of `tungsten-installer`, which allows you to change any property in the properties file.

For example, let's suppose that you don;t want the replicator to go ONLINE automatically when it starts. This behavior is controlled by a property called `replicator.auto_enable`, which is true by default. Of course, if you install the replicator with the --start option, you won't get a chance of modifying the property, because the replicator will be already online.

To achieve your purpose, you will add this option to the installation command:
```
    --property=replicator.auto_enable=false
```

## Restrict replication to specific schemas and tables ##

To forward or ignore operations on entire schemas or specific tables, use the 'replicate' filter.  The filter works from two comma-separated lists of tables and/or schema names.  You can use `*` and `?` as wildcards; these match strings and single characters respectively.

The 'do' filter list contains tables that should specifically be replicated.  The 'ignore' filter contains tables that should be ignored.  Here is an example of how to replicate only tables in schema test or tables named foo in any schema.  The --property option is quoted to prevent accidental shell wildcard expansion.

```
./tools/tungsten-installer --master-slave -a \
  ...
  --svc-extractor-filters=replicate \
  "--property=replicator.filter.replicate.do=test,*.foo" \
  ...
  --start-and-report
```

The following example drops tables foo and bar in schema tpc:

```
./tools/tungsten-installer --master-slave -a \
  ...
  --svc-extractor-filters=replicate \
  --property=replicator.filter.replicate.ignore=tpc.foo,tpc.bar \
  ...
  --start-and-report
```

Important note:  Due [Issue 258](https://code.google.com/p/tungsten-replicator/issues/detail?id=258) the replicate filter currently only works out of the box on statements if it is an extract filter.  You can work around [Issue 258](https://code.google.com/p/tungsten-replicator/issues/detail?id=258) by adding the eventmetadata filter to the chain so that it runs before the replicate filter.  The replicate filter works in any stage for row updates.

## Use a different schema name in the slave ##

If you want to replicate schema _stores_ in the master to schema _playground_ in the slave, you can use the **dbtransform** filter.

If you are installing a slave from scratch, you can add the following options to your tungsten-installer command:

```
    -a --svc-applier-filters=dbtransform \
    --property=replicator.filter.dbtransform.from_regex1=stores \
    --property=replicator.filter.dbtransform.to_regex1=playground \
```

If you had already installed the slave, you can achieve the same result with:

```
./tools/configure-service \
    -U \
    --release-directory=/where/tungsten/is/installed  \
    -a --svc-applier-filters=dbtransform \
    --property=replicator.filter.dbtransform.from_regex1=stores \
    --property=replicator.filter.dbtransform.to_regex1=playground \
    SERVICE_NAME
```
And then restart the replicator.

Sample session:
```
slave> create schema playground;

master> create schema stores;
master> use stores;
master> create table mytable (i int);

master> select table_schema, table_name from information_schema.tables where table_name="mytable"
+--------------+------------+
| table_schema | table_name |
+--------------+------------+
| stores       | mytable    |
+--------------+------------+

slave> select table_schema, table_name from information_schema.tables where table_name="mytable"
+--------------+------------+
| table_schema | table_name |
+--------------+------------+
| playground   | mytable    |
+--------------+------------+

```


## Use different table names in the slave ##

Similarly to the examples above, you can transform table names on the fly. Here's an example that transforms all tables of form xxx\_ab\_yyy to prefix\_ab:

```
    -a --svc-applier-filters=tabletransform \
    --property=replicator.filter.tabletransform.from_regex1='.+_([a-z][a-z])_.+' \
    --property=replicator.filter.tabletransform.to_regex1='prefix_$1' \
```

## Add one slave to an existing master ##

The procedure is almost the same used to create a master-slave cluster. You use a similar command, with `--master-slave` and `--master-host` as in the full cluster installation command. The difference is that the `--cluster-hosts` list will only contain the slave host. Tungsten will do the right thing.

## Start a master service with a given binlog and position ##

The easiest way of starting the master service at a given binlog file and position is by using

` --master-log-file=mysql-bin.000045 --master-log-pos=10292 `

(Note: there is a problem with this procedure as documented in  [Issue#216](http://code.google.com/p/tungsten-replicator/issues/detail?id=216).

If the replicator has already been installed and we want to start replicating from a given binlog file and position, we can do this:

```
    trepctl -host host_name -service service_name offline 
    trepctl -host host_name -service service_name online -from-event 000045:10292
```
Notice that we don't give the complete binlog file name, but only the extension. The number after the colon is the position.

If you are installing a new copy of the replicator, run `tungsten-installer` with `--auto-enable=false` to keep the replicator from going online the first time.  After you have put the replication service online, turn on auto-enable by running `tools/configure-service -a -U --auto-enable=true service_name`.

Notice that there is a dedicated script for this task in the ./cookbook directory. (add\_node\_master\_slave.sh)

## Modify the configuration template file prior to configuration ##

There are advanced scenarios where you would like to modify the source template files or even add additional templates to the code.  One of the simplest is when you need to add a new filter definition.  We recommend that you use diff/patch to modify the release package prior to configuration.

  * `cp tungsten-replicator-2.0.5 tungsten-replicator-2.0.5-diff`
  * Make changes in tungsten-replicator-2.0.5-diff
  * `diff -ruN tungsten-replicator-2.0.5 tungsten-replicator-2.0.5-diff > t-r-2.0.5.diff`
  * Apply the patch to the release package
    * `patch -p1 -dtungsten-replicator-2.0.5 < t-r-2.0.5.diff`
    * `cat t-r-2.0.5.diff | patch -p1 -dtungsten-replicator-2.0.5`
    * 
```
echo "diff -Nru tungsten-replicator-2.0.5/tungsten-replicator/samples/conf/filters/default/sample.tpl tungsten-replicator-2.0.5-diff/tungsten-replicator/samples/conf/filters/default/sample.tpl
--- tungsten-replicator-2.0.5/tungsten-replicator/samples/conf/filters/default/sample.tpl    1969-12-31 19:00:00.000000000 -0500
+++ tungsten-replicator-2.0.5-diff/tungsten-replicator/samples/conf/filters/default/sample.tpl 2011-09-16 12:13:01.000000000 -0400
@@ -0,0 +1,2 @@
+# Sample filter
+replicator.filter.sample=com.continuent.tungsten.replicator.filter.SampleFilter
\ No newline at end of file" | patch -p1 -dtungsten-replicator-2.0.5
```
  * `tungsten-replicator-2.0.5/tools/tungsten-installer ...`

Up to [the Tungsten Replicator Cookbook](TungstenReplicatorCookbook.md)