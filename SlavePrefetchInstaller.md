# Slave Prefetch Design and Operation #

## Overview ##
Slave pre-fetching is a feature that reads ahead of the slave applier, and warms up the pages needed by the applier by converting THL updates into selects and executing them a few seconds before the slave applies the change. This way, the page is already in memory by the time the change is applied.

## Implementation ##
The pre-fetcher is a replicator service that can theoretically be installed in two ways: either as a service to an already existing replicator or as a separate replicator. This second one, while likely less efficient, is possibly more common, as it can be used to boost a Tungsten Enterprise slave or (eventually) to speed up a MySQL native replication slave by reading the relay logs and applying from them.
Either way, the pre-fetching replicator service will not write anywhere in the database, will not have its own service database, and will borrow connecting credentials from another replication service.

Note: for now only separate prefetch replicator processes are supported in the installation.

### Pipeline for Pre-fetching ###

The prefetch pipeline has the following characteristcs.
  * Connection parameters are the same as the slave that needs to be boosted.
  * There is a new role, **prefetch** that will set the defaults for this case.
  * The pipeline reads from the slave THL, store in a "PrefetchStore", then apply using an in-memory parallel queue using a "PrefetchApplier."
  * The PrefetchStore manages flow control for prefetching.  It watches the position of the slave in the trep\_commit\_seqno table and keeps the slave from getting too far ahead.
  * The PrefetchApplier contains logic to fetch on the primary key of
any affected row as well as any secondary indexes.

Prefetching currently only works on slaves that are **not** using parallel replication.  For now you must either use parallel replication or prefetching, but not both.  Prefetching is therefore suited only for single-threaded slaves, which block due to I/O.

### Configuration Description ###

**NOTE**  This section is obsolete.  Please check current .tpl files for replication.properties.prefetch-slave for proper values.

The snippet below is an example of the changes made to the static properties file.  It shows how to define a prefetch pipeline, configure, the prefetch applier, and the prefetch store, which controls the rate at which transactions are fed to the parallel queue in the second stage.

Here is the overall prefetch pipeline.
```
# PREFETCH PIPELINE:  three stages:  extract from remote THL to prefetch 
# queue store, then extract to parallel queue, then from parallel queue 
# to DBMS extract from local THL to queue; apply from queue to DBMS.
replicator.pipeline.slave-prefetch=remote-to-q,q-to-pq,pq-to-dbms
replicator.pipeline.slave-prefetch.stores=prefetch-queue,parallel-queue
replicator.pipeline.slave-prefetch.syncTHLWithExtractor=false
replicator.pipeline.slave-prefetch.autoSync=true

replicator.stage.remote-to-q=com.continuent.tungsten.replicator.pipeline.SingleThreadStageTask
replicator.stage.remote-to-q.extractor=thl-remote
replicator.stage.remote-to-q.applier=prefetch-queue
replicator.stage.remote-to-q.filters=

replicator.stage.q-to-pq=com.continuent.tungsten.replicator.pipeline.SingleThreadStageTask
replicator.stage.q-to-pq.extractor=prefetch-queue
replicator.stage.q-to-pq.applier=parallel-q-applier
replicator.stage.q-to-pq.blockCommitRowCount=${replicator.global.buffer.size}

replicator.stage.pq-to-dbms=com.continuent.tungsten.replicator.pipeline.SingleThreadStageTask
replicator.stage.pq-to-dbms.extractor=parallel-q-extractor
replicator.stage.pq-to-dbms.applier=dbms
replicator.stage.pq-to-dbms.taskCount=${replicator.global.apply.channels}
replicator.stage.pq-to-dbms.blockCommitRowCount=${replicator.global.buffer.size}
```

Here is the prefetch store definition
```
# Prefetch queue storage.  
replicator.store.prefetch-queue=com.continuent.tungsten.replicator.prefetch.PrefetchStore

# Buffer size for internal queue.  Larger values (i.e., over 100 have little
# impact on performance. 
replicator.store.prefetch-queue.maxSize=${replicator.global.buffer.size}

replicator.store.prefetch-queue.url=jdbc:mysql:thin://${replicator.global.db.host}:${replicator.global.db.port}/tungsten_parallel

# Name of the catalog schema of the slave for which we are prefetching. 
replicator.store.prefetch-queue.slaveCatalogSchema=tungsten_parallel

# Minimum number of seconds latency on prefetched events. 
replicator.store.prefetch-queue.minTimeAhead=3

# Maximum number of seconds to precede slave.  
replicator.store.prefetch-queue.maxTimeAhead=120

# Time *in milliseconds* that prefetcher should sleep if it gets too far ahead
# of the slave. 
replicator.store.prefetch-queue.sleepTime=200

# If set to true allow all events to be prefetched.  This aids debugging
# as it causes all events to flow through the prefetcher regardless of the
# slave position. replicator.store.prefetch-queue.allowAll=false
```

Finally, here is the prefetch applier definition.
```
# Prefetch applier.  This applier depends on a PrefetchStore to handle
# flow control on transactions that require prefetching. 
replicator.applier.dbms=com.continuent.tungsten.replicator.prefetch.PrefetchApplier

# URL, login, and password of Tungsten slave for which we are prefetching. 
# The URL must specify the replicator catalog schema name. 
replicator.applier.dbms.url=jdbc:mysql:thin://${replicator.global.db.host}:${replicator.global.db.port}/
replicator.applier.dbms.user=${replicator.global.db.user}
replicator.applier.dbms.password=${replicator.global.db.password}

# Slow query cache parameters.  Slow queries include those with large
# numbers of rows or poor selectivity, where selectivity is the
# fraction of the rows selected.  Any query that exceeds these is
# not repeated for the number of seconds in the cache duration property.  
replicator.applier.dbms.slowQueryCacheSize=10000
replicator.applier.dbms.slowQueryRows=1000
replicator.applier.dbms.slowQuerySelectivity=.05
replicator.applier.dbms.slowQueryCacheDuration=60
```

All other property file values remain the same.

### Parallelization in Prefetch Pipelines ###

Prefetch pipelines do not have their own THL and do not read the slave THL directly.  For this reason, parallelization uses an in-memory queue with a RoundRobinPartitioner.  This eliminates the necessity for disk management and also works if the prefetch replicator is remote from the slave.

### Pre-Fetch Query Generation ###

Pre-fetch may require multiple queries to read data pages for a single update operation.  Let's assume the following example table.

```
CREATE TABLE `appuser` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `last_name` varchar(128) DEFAULT NULL,
  `first_name` varchar(128) DEFAULT NULL,
  `office_id` int(11) DEFAULT NULL,
  `login` varchar(32) DEFAULT NULL,
  `salary` int(11) DEFAULT NULL,
  `creation_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `appuser_idx_name` (`last_name`,`first_name`),
  KEY `appuser_idx_login` (`login`),
  KEY `appuser_fk_1` (`office_id`),
  CONSTRAINT `appuser_fk_1` FOREIGN KEY (`office_id`) REFERENCES `office` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1
```

To pre-fetch properly on this table we need to generate queries to fetch the following kinds of pages into the buffer cache:

# Primary keys and leaf rows.  We need to select each row that will be written into memory.
# Secondary index pages.  We need to force reading of pages on affected secondary indexes such as appuser\_idx\_name.  These pages may be used to find data in the first place and will certainly need to be present if they are updated.  This can be accomplished by issuing queries that use the secondary index as a covering index.
# Foreign keys.  Foreign key constraints require that affected primary key pages on referenced tables should be available.

The next three subsections describe how to generate appropriate prefetch queries.

#### Pre-fetch for INSERT ####

We can prefetch on INSERT statements as follows.  Assume the following sample statement:

```
insert into appuser 
  (last_name, first_name, office_id, login, salary, creation_date)   
     values ('smith', 'bob', 23, 'bobsmith', 25000, now());
```

  1. Generate a SELECT on the primary key, if known.  This loads the page that will receive the row.  For auto\_increment keys like the sample this is not possible.
  1. If the INSERT uses a SELECT to fetch data from another table, execute that SELECT statement.
  1. Issue covering queries on all matching indexes, for example: `SELECT count(*) from appuser force index(appuser_idx_login) where login='bobsmith'`
  1. Issue queries to fetch foreign key values on their original tables, for example:  `SELECT id from office where id=23`

#### Pre-fetch for UPDATE ####

We can prefetch on UPDATE statements as follows.  Assume the following update statement:

```
update appuser set last_name='brown' where id=352
```

  1. Generate a SELECT on the update where clause.
  1. If the UPDATE uses a sub-select to fetch data from another table, execute that SELECT statement.
  1. Issue covering queries on all matching indexes, for example: `SELECT count(*) from appuser force index(appuser_idx_name) where last_name='brown'`
  1. Issue queries to fetch affected foreign key values on their original tables, for example:  `SELECT id from office where id=23`

Update pre-fetch is somewhat inefficiently when using statement replication as we cannot see the affected values easily.

#### Pre-fetch for DELETE ####

We can prefetch on DELETE statements as follows.  Assume the following statement:

```
delete from appuser where id=352
```

  1. Generate a SELECT on the where clause used to delete rows.  Record the data received from the query to generate additional queries.
  1. Issue covering queries on all matching indexes, for example: `SELECT count(*) from appuser force index(appuser_idx_name) where last_name='smith' and first_name='bob'`
  1. If there are cascading deletes due to foreign key relationships we should presumably issue selects on those tables as well using the same logic.

### Low-Cardinality Indexes and Slow Query Handling ###

Queries on secondary indexes can be very slow if the index has low cardinality, i.e., a small number of unique values.   Such indexes can have an enormous number of entries per value if the base table is large.  For example, an index with 100 unique values on a 10M row table would average 100K entries per index value.

Prefetch queries on low cardinality indexes slow down the entire prefetch operation if we repeat them constantly.  The prefetch applier therefore maintains a slow query cache, which "remembers" queries that either count a large absolute number of rows or a high fraction of the table.  The cache parameters and their meanings are described below.

| **Cache Property** | **Meaning** |
|:-------------------|:------------|
| slowQueryCacheSize | Maximum number of query records to cache at any time |
| slowQueryRows      | Queries that count or return more than this number of rows go into the cache |
| slowQuerySelectivity | Queries that count or return this fraction or more out of the total estimated rows go into the cache |
| slowQueryDuration  | Number of seconds to wait before repeating slow queries |

The default values should be OK for most purposes.  If the prefetcher cannot keep up with the slave and skips a lot of rows as shown by `trepctl status -name stores` you may want to consider increasing the cache size.

The current settings of the cache as well as other prefetch statistics are dumped to the replicator log whenever the prefetch pipeline goes offline.  You can put the replicator offline and online again to see recent statistics.

## User interface ##

The tungsten-installer has been extended to install a prefetch pipeline that reads from a slave.  This downloads transactions from the slave using the slave's THL port.  The prefetch pipeline has no local storage of its own.

### Common properties ###
In all the configurations, users can tune the pre-fetching service with the following installation options:

| **Install option** | Meaning |
|:-------------------|:--------|
| prefetch-enabled   | If true set the service up as a prefetch applier |
| prefetch-max-time-ahead | Max seconds prefetch can precede the slave applier.  Prefetch sleeps to let the slave catch up when it hits this limit |
| prefetch-min-time-ahead | Minimum seconds prefetch should precede slave applier.  Prefetch discards events to get ahead when it hits this limit |
| prefetch-sleep-time | Number of milliseconds to sleep when the prefetch applier gets too far ahead |
| prefetch-schema    | Catalog schema of slave for which we are prefetching |

### Installation Command to Read from Remote THL ###

The following command shows a typical command to prefetch from a slave by downloading from the slave THL.  This command installs a completely separate replicator process and can be used for both Tungsten 2.0 as well as Tungsten 1.3 replicators.

```
$ tools/tungsten-installer --prefetch -a \
  --service-name=prefetch \
  --hosts=127.0.0.1 \
  --prefetch-enabled=true \
  --prefetch-schema=tungsten_parallel \
  --datasource-port=33307  \
  --datasource-user=msandbox  \
  --datasource-password=msandbox  \
  --home-directory=/opt/prefetch \
  --svc-parallelization-type=disk \
  --buffer-size=100 \
  --channels=5 \
  --rmi-port=10020 \
  --skip-validation-check=MySQLPermissionsCheck \
  --skip-validation-check=MySQLApplierServerIDCheck \
  --start-and-report

```

This creates a local prefetch installation in /opt/prefetch that uses RMI port 10020 for management.  It will connect to the local slave replicator on THL port 2112.  The usual datasource parameters apply.  This example is using a slave running on a sandbox.

If the THL is remote or has another port than 2112, you must override the THL connectUri property as shown in the following example.  This is typical for sandbox operation.

```
--property=replicator.extractor.thl-remote.connectUri=thl://127.0.0.1:2488/
```

If you want to force all transactions to go through the prefetcher for debugging purposes and not filter anything out, you should override the prefetch store allowAll property, as shown below.  This is useful for debugging as the prefetch pipeline will not filter any transactions but will try to prefetch on everything, regardless of whether already applied.

```
 --property=replicator.store.prefetch-queue.allowAll=true
```