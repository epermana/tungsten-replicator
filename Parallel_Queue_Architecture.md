# Introduction #

## Overview ##

Tungsten Replicator uses _parallel stores_ to implement pipeline stages in which multiple tasks read and apply transactions concurrently.  The most significant application of this technique is known as _parallel apply_, which speeds up replication by executing slave updates on different shards in parallel.  Parallel stores split serialized transactions into multiple output queues, which is why we sometimes call them _parallel queues_.

This wiki page summarizes the current parallel store implementations.  It is not end-user documentation but rather describes the algorithms behind the code as it currently stands and illustrates the trade-offs between different parallel store approaches.  The goal is to provide orientation when reading the code, designing tests, and developing new applications for parallel stores.

## Caveats ##

The Tungsten solution to parallel replication has developed incrementally over a period of a couple of years.  Our understanding of the problem and solutions has evolved considerably during this time.

This means that while the overall model and general algorithms are fairly consistent, some of the interfaces and naming for individual classes are not.  There will consequently need to be some clean-up over time to rationalize naming and restructure interfaces.  Also, reader can expect some additional iteration as we gain more understand of solutions and modify implementations in response to performance testing and production usage of parallel replication.

## Additional References ##

For more information on the overall approach to parallel replication in Tungsten, see the following:

  * [Continuent Product Documentation](http://www.continuent.com/downloads/documentation)

  * [Parallel Replication on MySQL: Report from the Trenches](http://scale-out-blog.blogspot.com/2010/10/parallel-replication-on-mysql-report.html)

  * [Parallelization Using Shards Is the Only Workable Approach for SQL](http://scale-out-blog.blogspot.com/2011/03/parallel-replication-using-shards-is.html)

# What We Mean by a Tungsten Parallel Store #

Tungsten Replicator uses a pipeline-based execution model, where pipelines are event flows that extract from a source and apply to a sink at the other end.  Pipelines consist of one or more stages, which implement extract-filter-apply loops to transfer events.  Stores sit between stages and buffer events as they pass through the pipeline.  Stores therefore act as queues within the pipeline and may be either persistent (like the THL) or non-persistent in-memory buffers.

A Tungsten parallel store is a type of [Store](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/src/java/com/continuent/tungsten/replicator/storage/Store.java) that demultiplexes a serialized store of events into multiple queues.  Parallel stores implement an additional [ParallelStore](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/src/java/com/continuent/tungsten/replicator/storage/ParallelStore.java) interface that provides control methods to enable safe shutdown.

The following diagrams shows how parallel stores work in general.  A single stage task delivers serialized events to the store.  The store demultiplexes the events into parallel queues that feed multiple tasks in the next stage.

![https://s3.amazonaws.com/extras.continuent.com/code.google.com-images/Tungsten-Parallel-Queue-Model.jpg](https://s3.amazonaws.com/extras.continuent.com/code.google.com-images/Tungsten-Parallel-Queue-Model.jpg)

Parallel threads are referred to as _channels_ in user documentation.  Channels are implemented so far as queues within the parallel store that serve corresponding tasks in the stage that extracts from the store.  Extractors on the parallel store therefore have an additional method documented in interface [ParallelExtractor](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/src/java/com/continuent/tungsten/replicator/extractor/ParallelExtractor.java) to set the task ID so that each task extracts from the correct queue.

ParallelStore implementations must deal with a number of practical issues that make the implementations a little harder than one might think at first.

  * Partitioning.  All current implementations call an instance of the [Partitioner](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/src/java/com/continuent/tungsten/replicator/storage/parallel/Partitioner.java) interface to split events into separate channels.  This is not formally part of the model though it probably should be.  Partitioning algorithms must always be idempotent and return the same channel assignment for a given number of channels.
  * Full serialization.  Some transactions cannot safely run in parallel.  Internally we call these critical events.  Parallel stores must be able to revert to fully serialized operation when a critical event appears.
  * Restart.  Tungsten marks the current seqno of each apply task (usually in the trep\_commit\_seqno table for SQL databases).  For single-threaded tasks, this is easy because there is a constant stream of events so we update the restart position at the end of each block.  However, with parallel operation we may find that some queues are empty for prolonged periods of time and do not mark position.  This causes a problem when a replicator crashes or is terminated, because the applier restarts at the least advanced position.  In pathological cases this can cause replicators to rescan millions of transactions to find the restart position.  Current implementations solve this by periodically inserting synthetic control events that cause the down streams tasks to update their position.
  * Clean shutdown.  Parallel stores need to be able to bring all channels to the same point and shut down.  This allows users to change the number of channels or revert back to non-parallel operation safely We currently handle this by inserting a stop control event on all channels.  When the downstream stage tasks receive this event they shut down.  Clean shutdown is complete when all stage tasks have shut down in this way.

There are many ways to implement this model in practice.  The following sections describe the current implementations.

# In-Memory Parallel Store #

## [ParallelQueueStore](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/src/java/com/continuent/tungsten/replicator/storage/parallel/ParallelQueueStore.java) ##

### Architecture ###

This implemention works by splitting applied events into a separate queue for each channel.  One task thread applies new events.  A task thread in the succeeding stage extracts from each individual queue.  The general flow is illustrated in the following diagram.

![https://s3.amazonaws.com/extras.continuent.com/code.google.com-images/Tungsten-ParallelQueueStore.jpg](https://s3.amazonaws.com/extras.continuent.com/code.google.com-images/Tungsten-ParallelQueueStore.jpg)

### Implementation Notes ###

The implementation partitions events into channels using a partitioner, as described in the general discussion of parallel stores.  All processing including putting events into queues and fetching them out again occurs in the task threads, which means that no additional threads are required within the store itself.

Queues are implemented using Java blocking queues, e.g., implementations of java.util.concurrency.BlockingQueue.  Each queue has a maximum size (maxSize property) and blocks when it is reached.  Blocking queues have the advantage that queue methods synchronize on the queue, which ensures that data are properly visible across queue reader and writer threads in accordance with guarantees of visibility provided by Java locks.

### Advantages ###

The ParallelQueueStore has a number of advantages.

  1. There is no latency between reading and writing, since everything is in-memory.
  1. It is relatively simple to implement correct serialization and clean shutdown in this model.
  1. The fact that no extra threads are necessary eliminates coordination complexity within the store and reduces the number of cases that need to be tested to confirm correct operation.
  1. There is no persistent state to manage if we crash and restart.

### Disadvantages ###

On the other hand there are problems with the in-memory design that make it difficult to use on some workloads.  The main reason is that even on systems with "nice" shard partitioning patterns across channels, events tend to show up in groups in different channels, so that at any given instant it is common to have many events on some channels and few or none on the rest.  This leads in turn to the following problems.

  1. You need to allocate enormous queue sizes to get good serialization.  Otherwise, one busy channel will fill up the overall parallel queue capacity, effectively serializing everything.  This turns the replicator into a bloated memory hog.  It is especially problematic with row replication, which generates an update for every row that changes.
  1. Even with large queues you still get serialization problems.  It is not uncommon for some SQL statements to execute very slowly, for example following a restart when DBMS caches are cold.  Consequently, you may again find that all channels block behind one or two slow statements.

Systems that process large numbers of transactions per second tend to see these problems in spades.  On a system that processes 1000/TPS 60 seconds of processing is 60K transactions, which in turn could be 200GB of memory in the Java heap.  This kind of math quickly leads to very large memory allocations to prevent serialization.

# Disk-Based Parallel Store #

## [THLParallelQueue](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/src/java/com/continuent/tungsten/replicator/thl/THLParallelQueue.java) ##

### Architecture ###

The THLParallelQueue implementation works by reading events from the THL using a thread for each channel and putting them into a queue that can be read by extract task threads.  The apply task applies events to the store, which allows the implementation to scan for serialization points.  The general flow is illustrated in the following diagram.

![https://s3.amazonaws.com/extras.continuent.com/code.google.com-images/Tungsten-THLParallelQueue.jpg](https://s3.amazonaws.com/extras.continuent.com/code.google.com-images/Tungsten-THLParallelQueue.jpg)

Note that this approach spawns an extra thread for each channel to read the THL and put events that belong to the channel in the corresponding read queue.

### Implementation Notes ###

The implementation uses a partitioner to assign channels.  This happens in two places.  First the apply task thread uses a partitioner to check the channel and look for serialization points, which it marks in a queue.  Second, channel read threads call the partitioner to decide whether to accept events when scanning the THL.

The channel read threads are implemented by class [THLParallelReadTask](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/src/java/com/continuent/tungsten/replicator/thl/THLParallelReadTask.java).  The task implementation includes a read loop that connects to the THL and does what appoints to a table scan using a predicate to discard events that do not belong to the channel.

The THLParallelQueue class handles synchronization between threads through a call-back to a global sync counter.  The counter increments each time the apply task from the preceding stage applies a new event.  This prevents threads from advancing ahead of the apply task that scans the THL.  It also enforces serialization points, in which one and only one read thread is allowed to advance while others wait.

Since we read events from disk, the actual in-memory queues are quite short.  We use Java blocking queues for this purpose for reasons already cited in the discussion of in-memory parallel queues.

### Advantages ###

The [THLParallelQueue](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/src/java/com/continuent/tungsten/replicator/thl/THLParallelQueue.java) implementation has the following benefits.

  1. There is no state in the implementation, which makes restart simple, as with a pure in-memory approach.
  1. It is possible for positions on particular channels to become quite widely separated without consuming additional memory.
  1. There are no extra writes required, and writes to the THL remain sequential, hence minimize the performance hit to write the log.

### Disadvantages ###

On the other hand there are several potential drawbacks to this approach.

  1. Thread initialization and coordination is tricky.  For instance, it is difficult to manage serialization points.  The logic for starting and stopping threads is somewhat difficult to understand.
  1. Performance depends on read threads being quite fast.  There are potentially many read tasks reading the THL.  We assume that the pages they need are present in the OS page cache, so these reads should be fairly quick.  This assumption remains to be proven in the field.  It seems unlikely to scale well beyond a small number of threads.

## Log File per Channel Alterative ##

### Design ###

Another approach, currently unimplemented, is to split the THL into multiple logs.  In this case the new logs would implement what amount to on-disk queues, from which extract tasks would read directly.  Here is an illustration.

![https://s3.amazonaws.com/extras.continuent.com/code.google.com-images/Tungsten-OnDiskLogPerChannel.jpg](https://s3.amazonaws.com/extras.continuent.com/code.google.com-images/Tungsten-OnDiskLogPerChannel.jpg)

### Implementation Notes ###

This approach looks very much like an in-memory queue; in fact if the interface were designed to look like a Java [BlockingQueue](http://download.oracle.com/javase/6/docs/api/index.html?java/util/concurrent/BlockingQueue.html) the code would look almost identical to the in-memory queue implementation, which would simplify maintenance.

This implementation requires the following extensions to the current THL.  Other than that it looks fairly straight-forward to implement.

  1. Extend protobuf event serialization to include the [ReplControlEvent](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/src/java/com/continuent/tungsten/replicator/event/ReplControlEvent.java) class.
  1. Add a "burn after reading" option to the log access API to drop log files automatically after they have been read.
  1. Optimize writes to ensure that they are remain performant when there are large numbers of open log files.  Otherwise writes may  devolve into a lot of inefficient random I/O.  Using SSD or RAM disk may be a solution.  Software approaches like file pre-allocation or effective write buffering may also help.

### Potential Advantages ###

Some of the potential advantages of log-per-channel include the following:

  1. Simple to understand, implement, and maintain, due to the conceptually clear model of using the log as a queue.
  1. Eliminates potential table-scan problem of [THLParallelQueue](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/src/java/com/continuent/tungsten/replicator/thl/THLParallelQueue.java).
  1. If logs are written fresh on each restart, this store is stateless. That avoids restart problems.

### Disadvantages ###

Here are the potential draw-backs.

  1. Simultaneous I/O on many open files is difficult to optimize well for all cases.  The actual performance is a bit difficult to predict without testing.  For example, it may perform poorly on standard 7200 SATA disks but better on RAID with a large controller queue or SSD.  Buffering writes to get more sequential access will also increase latency through the pipeline.
  1. The fact that logs are written to disk may slow start-up in cases where we need to rescan a lot events or where there are many channels.