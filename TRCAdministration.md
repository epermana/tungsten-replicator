
Up to [the Tungsten Replicator Cookbook](TungstenReplicatorCookbook.md)


# Administration #

## Check replication services ##

The tool that helps you look inside the replicator is called `trepctl`, and it's located under $TUNGSTEN\_HOME/tungsten/tungsten-replicator/bin.

To get a list of the existing services, run:

```
TUNGSTEN_BIN=$TUNGSTEN_HOME/tungsten/tungsten-replicator/bin

$TUNGSTEN_BIN/trepctl services
```


## Check replication status ##

The 'status' option of trepctl will give you detailed information about the health of the replicator.
```
$TUNGSTEN_BIN/trepctl -service service_name status
```


## Suspend and resume replication ##

To suspend a replicator service, put it offline:

```
$TUNGSTEN_BIN/trepctl -service service_name offline
```

Notice that this operation will not affect other services, if your replicator is running more than one.

To put it back online, you change the keyword.

```
$TUNGSTEN_BIN/trepctl -service service_name online
```

You may go online with several options, such as skipping one or more transactions, or replicating from a different file/position.

### Resuming replication and skipping a transaction ###

```
$TUNGSTEN_BIN/trepctl -service service_name online -skip-seqno 2340
```

## Connecting a Slave with Event ID after Loading a Dirty Snapshot ##

Dirty snapshots are backups taken on a master where the DBMS does not match the contents of the trep\_commit\_seqno.  This situation comes up when provisioning slaves as well as recovering failed masters from file system snapshots.

Here is an example.  You have a master DBMS with Tungsten Replicator running as a master.  You use a file system snapshot to capture the master DBMS state and load it to a slave.  However, when it is time to connect the slave to the Tungsten master, your snapshot might have a value in trep\_commit\_seqno that does not quite match the state of the database.

Here is how to connect the slave on MySQL.  We assume Tungsten is configured but offline on the slave.

1. After MySQL reboots, look in the error log.  You will see a line like the following in the log messages:

```
InnoDB: Last MySQL binlog file position 0 370418394, file name /mysqlLogs/mysql-bin.002106
```

2. Bring the master online using the master binlog position from the error log.

```
trepctl online -from-event mysql-bin.002106:370418394
```

Tungsten will seek forward in the master log and start replicating from the first transaction after this binlog position.

**Note:** Tungsten only seeks forward in the master log.  You cannot specify an event ID that is before the seqno in trep\_commit\_seqno.

## Inspect Transaction History Logs ##

The THL (Transaction History Logs) is the data that Tungsten has taken from the master's binary logs and transported to its servers, with the addition of some metadata.

There is a tool, called 'thl', which allows you to see the status of the logs and eventually list its contents.

```
$TUNGSTEN_BIN/thl -service service_name info
```
This command will give you a summary of what you may find in the THL.

```
$TUNGSTEN_BIN/thl -service service_name index
```
Similar to the previous one, it will give you a list of the available THL files, with the minimum and maximum transaction number for each file.

```
$TUNGSTEN_BIN/thl -service service_name list
```
This command will list all the transactions that are available for the given service. This is potentially dangerous, since it may list millions of transactions on your screen. See the next ones for more options.

```
$TUNGSTEN_BIN/thl -service service_name list -low 0 -high 1858
$TUNGSTEN_BIN/thl -service service_name list -seqno 34902
```
The first command lists transactions in a given range, while the second lists only one specific transaction.

## Check parallel replication status ##

### Shards ###

```
$TUNGSTEN_BIN/trepctl -service service_name status -name shards
```

### Tasks ###

```
$TUNGSTEN_BIN/trepctl -service service_name status -name tasks
```

### Stores ###

```
$TUNGSTEN_BIN/trepctl -service service_name status -name stores
```

## Resetting the Transaction History Logs ##

The THL can be cleared in order to reset the replication service and start from a clean slate.

  * Suspend replication
```
$TUNGSTEN_BIN/trepctl -service service_name offline
```
  * Remove all existing THL files
```
rm $TUNGSTEN_HOME/thl/service_name/*
```
  * Remove all existing relay files
```
rm $TUNGSTEN_HOME/relay/service_name/*
```
  * Reset the replication service schema
```
TRUNCATE TABLE tungsten_service_name.trep_commit_seqno;
TRUNCATE TABLE tungsten_service_name.heartbeat;
```
  * Resume replication
```
$TUNGSTEN_BIN/trepctl -service service_name online
```

## Upgrading from Tungsten Replicator 2.0.4 to 2.0.5 ##

Tungsten Replicator is released quite frequently. We add planned features and fix bugs continuously. Therefore, getting [the latest release](http://s3.amazonaws.com/files.continuent.com/builds/nightly/tungsten-2.0-snapshots/index.html) is very important.

If you have installed Tungsten Replicator 2.0.4 or later, and you want to upgrade to the latest version, here's what to do.

After you [get the replicator](TRCBasicInstallation#Get_the_replicator.md), run these commands for each host:

```
ssh host_name $TUNGSTEN_HOME/tungsten/tungsten-replicator/bin/replicator stop
./tools/update --host=host_name \
   --user=$USER \
   --release-directory=$TUNGSTEN_HOME
 
$TUNGSTEN_HOME/tungsten/tungsten-replicator/bin/trepctl -host host_name services
```

For example, if you have installed a regular master slave cluster using [this recipe](TRCBasicInstallation#Install_a_master_/_slave_cluster.md), the release directory will be $HOME/replication.

If the replicator is offline after the update command (that depends on your settings), you can simply put it online using trepctl.


**Notes:**
  * Upgrading from previous version of Tungsten Replicator requires a manual re-installation, because of the new installer format.
Upgrading to the next version of Tungsten should be handled in the same way described in this section.
  * The host used in the above commands must be the same used when the server was defined. If a host can be addressed by two names (e.g _m1.mynetwork.com_ and _m1_) the same name that was used during installation must also be used for the upgrade.

## Increasing the JVM memory size ##

The `--java-mem-size` option sets the amount of memory allocated to the JVM.  You can update the allocation on an existing replicator using the `tools/update` script.

```
$TUNGSTEN_HOME/tungsten/tools/update -a --java-mem-size=1024
$TUNGSTEN_HOME/tungsten/tungsten-replicator/bin/replicator restart
$TUNGSTEN_HOME/tungsten/tungsten-replicator/bin/trepctl services
```

## Managing replicator log space ##

There are 3 kinds of logs used in Tungsten Replicator.
  * Master Relay Logs
  * Transaction History Logs
  * Service Logs

### Master Relay Logs ###

The Master Relay Logs (MRL) are used when the master is extracting events from a MySQL server.  The MRL files are stored in the `$TUNGSTEN_HOME/relay/service_name` directory.  Use a symlink if you would like to store these files on a different disk.

The `replicator.extractor.dbms.relayLogRetention` value in `tungsten-replicator/conf/static-service_name.properties` defines how many MRL files are kept after the replicator service has extracted events from it.  You can configure this setting using the `--property` flag.

```
# New Installation
./tools/tungsten-installer ... --property=replicator.extractor.dbms.relayLogRetention=4

# Existing Installation
$TUNGSTEN_HOME/tools/configure-service -U --property=replicator.extractor.dbms.relayLogRetention=4 service_name
$TUNGSTEN_HOME/tungsten-replicator/bin/trepctl -service service_name stop
$TUNGSTEN_HOME/tungsten-replicator/bin/trepctl -service service_name start
$TUNGSTEN_HOME/tungsten-replicator/bin/trepctl -service service_name status
```

### Transaction History Logs ###

The Transaction History Logs (THL) are used to store events after they are extracted, to transmit events between the master and slave and to store events prior to being applied.  The THL files are stored in the `$TUNGSTEN_HOME/thl/service_name` directory.  Use a symlink if you would like to store these files on a different disk.

The `replicator.store.thl.log_file_retention` value in `tungsten-replicator/conf/static-service_name.properties` defines how long THL files are kept.  You can configure this setting using the `--property` flag.

```
# New Installation
./tools/tungsten-installer ... --property=replicator.store.thl.log_file_retention=3d

# Existing Installation
$TUNGSTEN_HOME/tools/configure-service -U --property=replicator.store.thl.log_file_retention=3d service_name
$TUNGSTEN_HOME/tungsten-replicator/bin/trepctl -service service_name stop
$TUNGSTEN_HOME/tungsten-replicator/bin/trepctl -service service_name start
$TUNGSTEN_HOME/tungsten-replicator/bin/trepctl -service service_name status
```

### Service Logs ###

The log files are stored in `tungsten-replicator/log`.  There are two service logs maintained by the replicator.

  * `user.log` - Partial service log that includes state changes and basic error messages
  * `trepsvc.log` - Complete service log including stack traces

The settings for these log files are located in `tungsten-replicator/conf/log4j.properties`.  You can modify the `log4j.appender.file.MaxFileSize` and `log4j.appender.file.MaxBackupIndex` values for each log file to manage how much disk space is required to hold the log files.

Check out the [TRCBasicInstallation#Modify\_the\_configuration\_template\_file\_prior\_to\_configuration](TRCBasicInstallation#Modify_the_configuration_template_file_prior_to_configuration.md) recipe for an idea of how you can update the `log4j.properties` file as part of installation.

## Avoiding or Fixing Corrupt Transaction History Logs ##

THL corruption is thankfully very rare but does strike on occasion.  This section contains information on how to avoid corruption in the first place and how to handle it when it does occur.

### Enabling THL Fsync ###

Transaction history logs by default merely flush THL records to the OS page cache rather than doing a full fsync each time the thread responsible for writing the log issues a commit.  The reason is that fsync operations are very slow on many storage types, and the THL can typically be recreated even if it is truncated due to a host crash.

If you would prefer to avoid race conditions that arise from such behavior, you should enable fsync on log flush, using the -thl-log-fsync option on tungsten-installer.  Here is a partial example:

```
tools/tungsten-installer ... \ 
  --thl-log-fsync=true \
  ...
```

When fsync is enabled, the THL flushIntervalMillis parameter takes effect.  By default it performs a flush every 500 milliseconds.  Fsync and delayed flushing can add considerable latency to replication, so you should only enable fsync if your system is lightly loaded or you have very fast storage.

### Enabling THL Checksums ###

The THL permits supports checksums on log records.  These detect corruption effectively but can add significant overhead for parallel replication in particular, as many threads may read and hence need to compute checksums on the same page.  The advantage of THL checksums is that they give you a clear error message when a bit accidentally "flips" in storage or the log gets corrupted, whereas without them the replicator behavior is unpredictable.  (It may just get confused and hang for instance.)

You can enable log checksums at installation time using the THL doChecksum property, as shown in the following partial example.

```
tools/tungsten-installer ... \ 
  --property=replicator.store.thl.doChecksum=true \
  ...
```

### Regenerating a Corrupted Slave THL ###

If you suspect full or partial slave log corruption, you can cure it as follows.  Full corruption means that the log is unusable starting at the first record or even completely deleted.  Partial corruption means that the end of the log is unusable but the log is good before some seqno value that you can find out reasonably easily.

Here is the standard procedure.

  1. Take the slave offline using 'trepctl offline'.
  1. Determine the last good seqno in the log, if there is one.
  1. Use 'thl purge -low nnn' to truncate the log, where nnn is the first bad seqno.  Or just remove the log directory entirely if the log is completely bad.
  1. Bring the slave back online using 'trepctl online'.
  1. The slave will get the missing log records from the master, then begin applying transactions at the last point recorded in the trep\_commit\_seqno table.

### Regenerating a Partially Corrupted Master THL ###

You can regenerate a partially corrupted master THL as follows.

  1. Take the master offline using 'trepctl offline'.  Slaves will move from ONLINE to SYNCHRONIZING.
  1. Determine the last good seqno in the log.
  1. Use 'thl purge -low nnn' to truncate the log at the last good seqno.
  1. Bring the master back online using 'trepctl online'.
  1. The master will re-extract the missing log records from the master DBMS.  Slaves will move from SYNCHRONIZING to OFFLINE-ERROR as the master epoch numbers in the newly generated log will be different from what the slaves expect.
  1. Bring any failed slaves back online using 'trepctl online -force'.  This skips the epoch number check and enables the slaves to reconnect.

The master case also applies if you are operating a direct pipeline.

### Regenerating a Fully Corrupted Master THL ###

You can regenerate a completely corrupted master THL as follows.  In this case we have to deal with the additional wrinkle that slaves already have a particular sequence number recorded, so you cannot just restart the log at seqno 0.  You must tell Tungsten where to start in the native log as well as which seqno value to start with.

  1. Take the master offline using 'trepctl offline'.  Slaves will move from ONLINE to SYNCHRONIZING.
  1. Search the slave trep\_commit\_seqno tables for the slave with the smallest seqno value.  Record the values of seqno and event ID from the slave table.
  1. Delete the master log directory.
  1. Bring the master back online using 'trepctl online -from-event _slave event ID_ -base-seqno _slave seqno_'
  1. The master will re-extract the missing log records from the master DBMS.  Slaves will move from SYNCHRONIZING to OFFLINE-ERROR as the master epoch numbers in the newly generated log will be different from what the slaves expect.
  1. Bring any failed slaves back online using 'trepctl online -force'.  This skips the epoch number check and enables the slaves to reconnect.

This case also applies if you are operating a direct pipeline and lose the master log completely.

# Monitoring and Troubleshooting #

## Monitor JVM memory usage ##

It is possible to use JConsole to monitor the memory usage of the Tungsten Replicator process.  If you see that the allocated memory is filling up, you may increase it using the {{--java-mem-size}} argument.

JConsole requires that jmxremote is enabled in `tungsten-replicator/conf/wrapper.conf`.  It is enabled by default but make sure that `wrapper.java.additional.3=-Dcom.sun.management.jmxremote` is not commented out in the file.

You can get information on JConsole at http://java.sun.com/developer/technicalArticles/J2SE/jconsole.html.

# Tungsten Limitations #

## Triggers and Row Replication ##

Tungsten Replicator does not automatically shut off triggers on slaves.  This creates problems on slaves when using row-based replication (RBR) as the trigger will run twice.  Tungsten cannot do this because the setting required to do so is not available to MySQL client applications. Typical symptoms are duplicate key errors, though other problems may appear.  Consider the following fixes:

1.) Drop triggers on slaves.  This is practical in fan-in for reporting or other cases where you do not need to failover to the slave at a later time.

2.) Create an "is\_master()" function that triggers can use to decide whether they are on the master or slave.

3.) Use statement replication.  Beware, however, that even in this case you may find problems with triggers and auto-increment keys.  For more look [here](http://bugs.mysql.com/bug.php?id=45677).

The is\_master() approach is simple to implement.  First, create a function like the following that returns 1 if we are using the Tungsten user, as would be the case on a slave.

```
create function is_master()
    returns boolean
    deterministic
    return if(substring_index(user(),'@',1) != 'tungsten',true, false); 
```

Next add this to triggers that should not run on the slave, as shown in the next example.  This suppresses trigger action to insert into table bar except on the master.

```
delimiter //
create trigger foo_insert after insert on foo
  for each row begin
    if is_master() then 
      insert into bar set id=NEW.id; 
    end if; 
  end;
//
```

As long as applications do not use the Tungsten account on the master, the preceding approach will be sufficient to suppress trigger operation.


# Role changing #

To switch roles in a master/slave topology, you use trepctl.

Let's assume that you have **node1**=master, **node2** and **node3** = slaves.

First, you need to block traffic going to node1. Then, you make sure that the slaves are in sync:

```
$ trepctl -host node1 flush
Master log is synchronized with database at log sequence number: 3245

$ trepctl -host node2 wait -applied 3245
$ trepctl -host node3 wait -applied 3245
```

After this, it's safe to put the replicators offline

```
$ trepctl -host node1 offline
$ trepctl -host node2 offline
$ trepctl -host node3 offline
```

and start the switch. We want to make node2 the new master.

```
$ trepctl -host node2 setrole -role master
$ trepctl -host node1 setrole -role slave -uri thl://node2
$ trepctl -host node3 setrole -role slave -uri thl://node2
```

Finally, we put back the replicators online:
```
$ trepctl -host node2 online
$ trepctl -host node1 online
$ trepctl -host node3 online
```


Up to [the Tungsten Replicator Cookbook](TungstenReplicatorCookbook.md)