# Overview #

The Transaction History Log, or THL, provides persistent storage of transactions.  It enables very fast transfer of transactions between locations while at the same time preventing data loss due to failures of hosts, processes, and DBMS software.

This wiki page provides insight into THL internals to help developers,  QA, and advanced users.  It is not product documentation.  For information on operating Tungsten Replicator check out the [regular product documentation](http://www.continuent.com/downloads/documentation).

If you plan to change the implementation in any way, please ensure you read the entire wiki page including the last sentence.

# Architecture #

## Embedded, Queue-Based Model ##

The THL functions as an embedded database within the replicator process.  It also runs inside the **thl** utility, which is a simple command-line tool to read the log.  The log files are protected by a file lock that permits multiple processes from writing to the log at the same time.

The THL is a log and therefore differs from traditional databases in the following ways.

  1. The THL is a queue.  It is is optimized for reading and writing sequentially.  A single thread  writes log records.  Any number of threads can read records.  The API is optimized to deliver written records quickly to waiting readers in a queue-like fashion.
  1. THL records are immutable.  Once written, THL records do not change.
  1. No buffer cache.  The THL currently depends on OS-level buffering.  It does not attempt to maintain its own page cache.

In other respects the THL behaves much like a pocket DBMS such as Derby or SQLLite, albeit without using SQL.  THL storage is self-maintaining.  There is a simple cursor-like API to seek to a specific position and step through records.

## Log Organization ##

### File Layout ###

Log files are stored in a single log directory, which is specified by the THL logDir parameter.  The following listing shows typical log file contents:

```
-rw-r--r-- 1 tungsten tungsten         0 2011-06-28 22:01 disklog.lck
-rw-r--r-- 1 tungsten tungsten 100000231 2011-06-28 22:15 thl.data.0000000001
-rw-r--r-- 1 tungsten tungsten 100006532 2011-06-28 22:58 thl.data.0000000002
-rw-r--r-- 1 tungsten tungsten   9731148 2011-06-28 23:04 thl.data.0000000003
```

The disklog.lck is a lock file.  The THL code must get an exclusive lock on this file to write to the log.  Otherwise the THL can only read data.  The lock file is always present event if no process is using it.  If you delete the lock file by accident, the THL code will create a new one the next time you open the file.

Data files have the form thl.data.nnnnnnnnnn, where the n's increase monotonically with each new log file.

### Log Record Formats ###

Log data files consist of primitive records, which in the current implementation come in three different flavors.  The primitive record formats are optimized for quick reading of header information without the need to invoke full object serialization.

#### Header Record ####

Each data file starts with a single header record that provides a magic number, version number, and previous log file sequence number.

| **Name** | **Format** | **Description** |
|:---------|:-----------|:----------------|
| magic\_number | 4 bytes, int | Log file identifier (x'C00lCAFE') |
| major\_version | 2 bytes, short | x'0001', Major version of log format |
| minor\_version | 2 bytes, short | x'0001', Minor version of log format |
| prev\_seqno | 8 bytes, long |Last seqno in previous log |

#### Event Record ####

Event records contain a header followed by a serialized ReplDBMSEvent.   The record format is as follows.

| **Name**        | **Format** | **Description** |
|:----------------|:-----------|:----------------|
| record\_length  | 4 bytes, int | Full length of record including record\_length field |
| record\_type    | 1 byte     | Event record type identifier (x'01') |
| header\_length  | 4 bytes, unsigned int | Length of the event header.  (Not used but may be in future) |
| seqno           | 8 bytes, unsigned long | Log sequence number |
| fragno          | 2 bytes, unsigned short | Event fragment number |
| last\_frag      | 1 byte     | x'01' or x'00' where x'01' denotes last fragment of event|
| epoch number    | 8 bytes, unsigned long | Event epoch number |
| source\_id      | null terminated UTF-8 string | Event source ID |
| event\_id       | null terminated UTF-8 string | Native DBMS event ID |
| shard\_id       | null terminated UTF-8 string | Name of the shard to which this event belongs |
| tstamp          | 8 bytes, unsigned long | Time of commit in milliseconds since 1970 (standard Java timestamp format) |
| data\_length    | 4 bytes unsigned int | Length of serialized event data |
| event           | Up to 2^32 - 1 bytes | Serialized Java object (e.g. ReplDBMSEvent) |
| crc\_method     | 1 byte     | Method used to compute CRC where x'00' means no CRC and x'01' means CRC-32 |
| crc             | 4 bytes, unsigned int | CRC of record excluding the CRC itself |

#### Log Rotation Record ####
Log rotation records are written at the end of the file log file.  They are a signal to clients to switch to the next log file.

| **Name**        | **Format** | **Description** |
|:----------------|:-----------|:----------------|
| record\_length  | 4 bytes, int | Full length of record including record\_length field |
| record\_type    | 1 byte     | Log rotation record type identifier (x'02') |
| next\_file\_name | null terminated UTF-8 string | Name of the next log file |
| crc\_method     | 1 byte     | Method used to compute CRC where x'00' means no CRC and x'01' means CRC-32 |
| crc             | 4 bytes, unsigned int | CRC of record excluding the CRC itself |

### Transaction Serialization ###

DBMS transactions are stored as serialized objects of the Java ReplDBMSEvent class, which Tungsten uses to represent transaction fragments in memory.   The default serialization method uses Google Protobuf, version 2.3.0. Google Protobuf uses message definition formats stored in protobuf/TungstenProtobufMessage.  We generate Java support code for serialization from this definition.

It is also possible to use Java serialization; earlier versions of the replicator before Tungsten 1.3 do so.  Java serialization is extremely verbose and may not handle upgrades well.  For this reason it is no longer used by default or even recommended for use.

### EPOCH Numbers ###

The EPOCH value is used a check to ensure that the logs on the slave and the master match. The EPOCH is stored in the THL, and a new EPOCH is generated each time a master goes online. The EPOCH value is then written and stored in the THL alongside each individual event. The EPOCH acts as an additional check, beyond the sequence number, to validate the information between the slave and the master. The EPOCH value is used to prevent the following situations:

  * In the event of a failover where there are events stored in the master log, but which did not make it to a slave, the EPOCH acts as a check so that when the master rejoins as the slave, the EPOCH numbers will not match the slave and the new master. The trapped transactions be identified by examining the THL output.

  * When a slave joins a master, the existence of the EPOCH prevents the slave from accepting events that happen to match only the sequence number, but not the corresponding EPOCH.Each time a Tungsten Replicator master goes online, the EPOCH number is incremented. When the slave connects, it requests the SEQUENCE and EPOCH, and the master confirms that the requested SEQUENCE has the requested EPOCH.

If not, the request is rejected and the slave gets a validation error:

```
pendingExceptionMessage: Client handshake failure: Client response validation failed: Log epoch numbers do not match: client source ID=west-db2 seqno=408129 server epoch number=408128 client epoch number=189069
```

When this error occurs, the THL should be examined and compared between the master and slave to determine if there really is a mismatch between the two databases.

### Indexing ###

The THL does not have a persistent index.  Instead an in-memory index is computed each time the log is opened.  The current implemention reads the header of each log data and opens and reads to the end of the last data file in order to compute the index.

Due to the fact that the implementation reads the last log file, you can improve start-up times by restricting log file size to 100Mb or less.  The THL code reads a file of this size in 2 seconds or so whereas reading a 1GB log data file may take 20 seconds or more depending on the speed of storage.

The in-memory log is less efficient for start-up but has the advantage that it is recomputed each time the replicator starts, hence is theoretically never out-of-sync with file contents.  This does mean, however, that if you delete a log file while the replicator is live the in-memory index will not know about it.  If the replicator needs to read the deleted file (for example when a slave connects) the THL behavior may be unpredictable.

You may safely delete old log files when the replication service is offline or the replicator process is stopped.  Automatic log file purging updates the in-memory index.

# Log Management #

The THL has user-settable configuration parameters as well as a utility to read and manage log records directly.

## Configuration ##

Current THL property settings are documented in the replication service template files.  For detailed information check out the Javadoc and source code, especially the following classes:

[com.continuent.tungsten.replicator.thl.THL](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/src/java/com/continuent/tungsten/replicator/thl/THL.java)

[com.continuent.tungsten.replicator.thl.log.DiskLog](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/src/java/com/continuent/tungsten/replicator/thl/log/DiskLog.java)

The following table provides a summary of current THL class properties and their meanings.

| **Property** | **Description** |
|:-------------|:----------------|
| bufferSize   | Buffer size used for reads and writes from/to storage.  This should never be less than the size of pages in persistent storage |
| doCheckSum   | If true, enable CRC checksums on records.  Can impact log performance by up to 50% if enabled but allows unambiguous detection of log record corruption. |
| eventSerializer | Name of event serialization class.  Normally not changed as it defaults to protobuf serialization. |
| flushIntervalMillis | Interval in milliseconds for flushing writes to disk, which in turn affects how quickly log readers see them, hence the applied latency on stages that read from the log |
| fsyncOnFlush | If true fsync to disk when flushing.  Log writes normally just do a Java flush() command, which makes writes visible to readers but does not guarantee they are fully stored |
| logConnectionTimeout | Timeout for dropping log connections and freeing file descriptors.  This value is obsolete and may be removed in future. |
| logDir       | Directory in which log files are stored.  Created automatically if it does not exist, though the parent directly of logDir must exist and be writable. |
| logFileRetention | Amount of time to retain log files, using a flexible time interface format.  "12h" means to preserve logs for 12 hours.  Based on the time log files are written, not the age of the transactions. |
| logFileSize  | Maximum number of bytes to write before rotating to a new log file.  The actual lengths of files can be greater as we never rotate in the middle of fragmented transactions. |
| password     | DBMS password where replication service catalog tables are stored |
| resetPeriod  | How often to reset the output stream used to transmit the stream of events from a THL server.  Larger values use network bandwidth more efficiently.  |
| URL          | JDBC URL of DBMS and schema where replication service catalog tables are stored |
| user         | DBMS user where replication service catalog tables are stored |

## 'thl' Utility ##

The **thl** utility looks scans and can optionally delete portions of the log.  It is described in the [Tungsten Replicator product documentation](http://www.continuent.com/downloads/documentation).

The following example shows the thl in use to seek a specific sequence number:

```
$ thl -service percona list -seqno 5
2011-07-01 15:15:24,883 INFO  thl.log.DiskLog Using directory '/opt/tungsten/thl-logs/percona/' for replicator logs
2011-07-01 15:15:24,883 INFO  thl.log.DiskLog Checksums enabled for log records: true
2011-07-01 15:15:24,883 INFO  thl.log.DiskLog Using read-only log connection
2011-07-01 15:15:24,888 INFO  thl.log.DiskLog Loaded event serializer class: com.continuent.tungsten.replicator.thl.serializer.ProtobufSerializer
2011-07-01 15:15:24,889 INFO  thl.log.LogIndex Building file index on log directory: /opt/rhodges4/thl-logs/percona
2011-07-01 15:15:24,897 INFO  thl.log.LogIndex Constructed index; total log files added=1
2011-07-01 15:15:24,898 INFO  thl.log.DiskLog Validating last log file: /opt/tungsten/thl-logs/percona/thl.data.0000000001
2011-07-01 15:15:24,898 INFO  thl.log.DiskLog Setting up log flush policy: fsyncIntervalMillis=0 fsyncOnFlush=false
2011-07-01 15:15:24,899 INFO  thl.log.DiskLog Idle log connection timeout: 28800000ms
2011-07-01 15:15:24,899 INFO  thl.log.DiskLog Log preparation is complete
SEQ# = 5 / FRAG# = 0 (last frag)
- TIME = 2011-07-01 15:15:17.0
- EPOCH# = 0
- EVENTID = 000868:0000000000001025;193351
- SOURCEID = logos2
- METADATA = [mysql_server_id=1;service=percona;shard=test]
- TYPE = com.continuent.tungsten.replicator.event.ReplDBMSEvent
- OPTIONS = [##charset = ISO8859_1, autocommit = 1, sql_auto_is_null = 1, foreign_key_checks = 1, unique_checks = 1, sql_mode = '', character_set_client = 8, collation_connection = 8, collation_server = 8]
- SCHEMA = test
- SQL(0) = insert into foo values(4) /* ___SERVICE___ = [percona] */
```

The **thl** internally uses exactly the same APIs as the replicator.  You can run read-only commands like **thl list** while a replicator is using the same log files but write operations like **thl purge** will be blocked by the file lock.  For this reason, you may not be able to bring a replicator online while processing a **thl purge** command as the replicator will not be able to get the read lock.

## Troubleshooting and Caveats ##

The THL is designed to be very reliable but there are still various ways to mess up logs, thereby resulting in considerable unhappiness.

### Deleting Log Files under Running Replicator ###

Replicator log files should only be deleted when the replication service is offline or the replicator process is completely stopped.  It is safe to delete older log files manually provided you do not leave gaps and provided no slave needs them.

### File Locks ###

The disklog.lck should never be deleted unless you want to clear logs completely.  If you delete the disklog.lck file while replication is running it removes the protection against a second process (either a replication service or **thl** command) modifying the files.  This would be very bad.

### Clearing Log Files ###

It is generally safe to clear log files completely while replication is offline or stopped.  This is recommended, for example, when restoring a backup to eliminate the possibility of the restored data conflicting with the log.

If you do clear logs observe the following caveats.

  1. Only clear logs on a master replicator service if the master DBMS is idle.  Clearing logs on a busy master and restarting replication may cause the replicator to extract transactions again.
  1. Clearing slave logs can cause problems for failover.  The slave will accumulate logs from the current seqno marked in the trep\_commit\_seqno table.  If you then promote the slave it may lack logs necessary for other slaves.  Don't forget that slaves may lag far behind the current master due to restart or reprovisioning from backup.

# Java APIs and Programming Model #

## Java Class Structure ##

The classes that implement the THL are spread out over the following packages.

| **Package** | **Description**|
|:------------|:|
| com.continuent.tungsten.replicator.thl | Adapts THL to pipeline interfaces (e.g., Store, Extractor, Applier) and to implement THL server |
| com.continuent.tungsten.replicator.thl.log | Log API and implementation classes |
| com.continuent.tungsten.replicator.thl.protobuf | Generated classes to implement serialization of events to/from Protobuf messages |
| com.continuent.tungsten.replicator.thl.serializer | Serialization adapter classes |

The thl package classes are tightly coupled with the rest of the replicator.  They cannot be separated from the rest of Tungsten.

On the other hand, the Log API in package com.continuent.tungsten.replicator.thl.log is largely self-contained and is the main interface for testing.  There are over 50 unit test cases on the public API and individual log implementation classes.  The goal of these test cases is to ensure that the log is fast and free from errors.

## Log Access APIs ##

You can find many examples of reading and writing to the THL using the log APIs within the Tungsten source code as well as unit tests like [DiskLogTest](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/test/java/com/continuent/tungsten/replicator/thl/log/DiskLogTest.java).  Here are a couple of examples that illustrate the programming model.

### Writing to the THL ###

To write records to the THL, get a [LogConnection](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/src/java/com/continuent/tungsten/replicator/thl/log/LogConnection.java) instance and issue store() methods.  Records always go to the end of the log, so you do not have to worry about positioning before writing.

```
// Open the log.  This creates if it does not exist. 
DiskLog log = new DiskLog();
log.setLogDir("/opt/tungsten/thl-logs/mylogDir");
log.setReadOnly(false);
log.prepare();

// Connect. 
LogConnection conn = log.connect(false);

// Write a transaction to the log. 
THLEvent thlEvent = createSomeTHLEvent();
conn.store(e, false);
conn.commit();

// Release the connection and the log. 
conn.release();
log.release();
```

### Reading from the THL ###

To read records from the THL, get a [LogConnection](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/src/java/com/continuent/tungsten/replicator/thl/log/LogConnection.java) instance, seek to where you would like to start reading, and issue next() calls to read records for as long as you would like.  By default next() waits for new records to show up, which makes the log behave like a queue.

```
// Open the log.  The log must exist. 
DiskLog log = new DiskLog();
log.setLogDir("/opt/tungsten/thl-logs/mylogDir");
log.setReadOnly(true);
log.prepare();

// Get a read-only connection. 
LogConnection conn = log.connect(true);

// Seek to a starting seqno in the log. 
if (! conn.seek(355))
{
    throw new Exception("Unable to find seqno: " + 355);
}

// Read successive records, waiting if necessary for them to appear
// in the log. 
while (true)
{
    THLEvent thlEvent = conn.next();
    // Do something with the event...
}

// Release the connection and the log. 
conn.release();
log.release();
```

This pattern is repeated widely in the THL code.

# Development #

## Protobuf Code Generation ##

You can regenerate Protobuf support classes using the 'ant protos' target in the replicator [build.xml](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/build.xml) file.  For this to work you must have the **protoc** version 2.3.0 binary in your path.

## Testing Changes ##

The THL is the single most critical component in the Tungsten replicator.  Errors in the log can lead to downed sites and mix up or corrupt user data.  The THL therefore has a wide range of test cases to ensure log correctness and catch regressions.  There are currently 55 Junit tests for the disk log classes alone.  The THL is also checked through extensive regression tests in the Continuent test suite.

Always run the unit tests and the fullest possible regression test to check changes to the log.  Unit tests should always be 100% clean.  Regression tests are typically less than 100% depending on the state of the product, but any test problems related to data consistency or log operation must be diagnosed prior to check-in.

Making changes to the THL without running every test possible is stupid.  Always extend the tests for new features and ensure _all_ tests are clean before you check in.