# Introduction #

Tungsten Replicator tags each transaction with a shard ID on extraction, which is then used in features like shard-based replication.  Shard properties are currently controlled using a file named shard.list, which is simple to understand but not manageable either remotely nor in a dynamic fashion.

This design describes a shard management interface that allows users to specify key shard properties dynamically through APIs.  Users may change shard properties on the fly and have them take effect through network calls only.

There are many applications of shards; this new API does not attempt to handle all of them.  Rather, it provides support for the existing parallel replication capabilities of the shard.list file and adds the ability to support "system-of-record" architectures for multi-master replication.  We will extend the API in future to support other types of applications efficiently.

# Background and Further Reading #

This design assumes readers are familiar with Tungsten Replicator architecture and operation, especially concepts like pipelines and replication services.  For more information please refer to the [Tungsten Replicator Guide 2.0](http://www.continuent.com/downloads/documentation).

# Requirements #

## Use Cases ##

### Overview ###

The initial implementation of the Shard Management API is designed to allow replication users to maintain primary and backup tenant copies across sites linked by replication.  This data storage model is known a "system of record" architecture where data processing for each schema occurs on a certain and replicates to backup copies on or more other sites.

To understand the model, imagine a software-as-a-service application with customer data stored in separate databases which Tungsten processes as shards.  The DBMS servers live on two separate sites, NYC and SJC, and are connected using bi-directional replication.  A change on NYC replicates to SJC; similarly a change on SJC automatically to NYC.

In the system of record model each customer has a primary site, which is where processing for that customer occurs.  Suppose company A has NYC as primary site.  All processing for that customer occurs on the primary site NYC and replicates automatically to the backup database copy on SJC.  Suppose customer B has SJC as primary site.  All processing for customer B occurs on SJC and replicates automatically to the backup database copy on NYC.

Within the same DBMS server, some customers may have NYC as the primary site, and some customers may have SJC as the primary site.  From time to time new customers are added.  Also, from time to time the primary site for a particular customer may switch.

The following diagram illustrates the system of record architecture using Tungsten Replicator replication services.

![https://s3.amazonaws.com/extras.continuent.com/code.google.com-images/bidi-replication-diagram.jpg](https://s3.amazonaws.com/extras.continuent.com/code.google.com-images/bidi-replication-diagram.jpg)

In this diagram each DBMS server has a local service and a remote service.  The local service reads the MySQL binlog and makes transactions available to other services.  The remote service pulls transactions from the local service on the other site.

To make the system of record architecture work, Tungsten Replicator must be extended to add rules that control replication of transactions across sites.  These rules must work to ensure that updates on the primary site replicate to the backup site.  Updates on the customer backup site, by contrast should not replicate back.  Finally, some schemas may be marked so that they do not replicate at all between either site.

The following use cases are supported.

### Use Case 1: Definition of New Customer ###

In this use case a new customer A is defined in the application with NYC as primary site.  The SaaS operators create a rule for the customer on the NYC remote service on the SJC site to accept customer A's shard "a".   They add a rule on the SJC remote service to drop customer A's shard.  After the rules are defined on both sites, processing for that customer may begin on the primary site.

### Use Case 2: Customer Data Replication ###

During normal operation an update on the customer A primary site NYC will replicate to the backup schema copy on the SJC backup site.  The update to the backup copy will appear in the backup site DBMS binlog and can replicate to slaves on the backup site.  However, the primary site NYC will will not accept the change back again.

### Use Case 3: Switching Customer Master between Sites ###

In this case the SaaS operators switch the customer primary site from NYC to SJC.  The SaaS operators first quiesce transactions for the customer in question on site NYC.  Once transactions have stopped **and** have replicated from the primary to the backup site, the SaaS operators will update shard processing rules to the reverse of Use Case 1 (accept transactions for shard "a" on NYC coming from SJC and refuse them from NYC to SJC).

### Use Case 4: Detect Discrepancies in Rules ###

SaaS operators may periodically check for discrepancies between shard processing rules in different services.  This is done by dumping the rules and diff'ing them.

## Detailed Requirements ##

The shard management API includes the following deliverables.

  1. A permanent shard management catalog for each Tungsten replication service that stores current metadata for shards.
  1. A network API to manage the shard catalog will be added.  It will allow users to add, edit, or remove shard definitions.
  1. Corresponding trepctl commands to make shard management easily accessible to scripts.
  1. A shard filter that manages transactions of particular shards.  This filter can be added to any replicator pipeline.  The shard filter will read the shard catalog each time the pipeline goes online.  The shard filter will support the following dispositions of shard transactions:
    * Accept.  Forward the transaction normally.
    * Drop.  Drop the transaction.
    * Warn.  Drop the transaction and generate a warning in the log.
    * Error.  Signal a replication error.  This will cause the replicator to go offline.
  1. Options to set the disposition of new shards.  These will be added automatically to the shard catalog with one of the options described.
  1. One or more status commands to dump the current state of the shard catalog and any relevant shard processing diagnostics.  These will be extensions to the current ‘trepctl status –name shards’ command.
  1. Updates to Tungsten documentation to cover management of shards and use of the provided interfaces.
  1. Configuration recommendations for setup of all shard management features and to ensure that Tungsten replicator can process a shard catalog change within 15 seconds on a loaded system.

# Implementation #
## Architecture ##

Shard management for the system of record model includes the following key components.

  * A new shard catalog table stored in the service database to track shards.
  * A new JMX MBean API for shard management with associated trepctl options to invoke APIs from scripts
  * A simple shard filter that processes shards based on shard metadata.  The shard filter implements options for generating new shard options.
  * A shard partitioner that uses the shard catalog table instead of shard.list.

There are also some miscellaneous changes to integrate shard information and configuration in other components.

## Shard Catalog Table ##

The shard catalog table is named trep\_shard.  It is automatically created in the service database used for replication if it does not exist.

The trep\_shard table is a public interface.  Users can select from this table to gain an accurate list of current rules for shard processing.  Users should **not** update the table as the values will not be propagated automatically to online replication services, which could lead to confusing errors.  Instead, users should use either the JMX API or trepctl to make changes.

The trep\_shard table contains the following columns.

| **Name**      | **Type**       | **Key** | **Description** |
|:--------------|:---------------|:--------|:----------------|
| name          | varchar(128)   | PRI     | Shard name      |
| master        | varchar(128)   |         | Home service name |
| critical      | char(1)        |         | If true, must serialize |

_Note_:  The 'critical' field may be dropped if we cannot link it in satisfactory way to the shard partitioning logic in classes like THLParallelQueue.

The master column determines the system that "owns" the shard.  The shard may only replicate from the data service name matches the master name.

## JMX API ##

Each replication service will offer an additional JMX MBean named ShardManagementMBean that contains operations for shard management.  The MBean will be published using a name derived from the service OpenReplicatorManagerMBean so that it is easy to find.

The shard management JMX interface is designed to be extremely simple.  There are four basic operations:  add, update, delete, and list.  These use list structures that contain a list of shards, where the shards themselves are stored as name-value pairs.

_Note_:  It may be convenient to introduce a call to set the shard
disposition only.

The ShardManagementMBean interfaces are described below.

```
public interface ShardManagerMBean
{
    /**
     * Returns true so that clients can confirm connection liveness.
     * 
     * @return true if the service is up and running, false otherwise
     */
    public boolean isAlive();

    /**
     * Inserts a list of shards into the shard table. Each shard will be a map
     * of name-value parameters, for example, name -> my_shard, channel -> 2.
     * 
     * @param params a list of shards to be inserted
     * @return The number of rows affected. 
     */
    public int insert(List<Map<String, String>> params);

    /**
     * Updates a list of shards into the shard table. Each shard will be a map
     * of name-value parameters, for example, name -> my_shard, channel -> 2,
     * the key to be used to update being the shard name.
     * 
     * @param params a list of shards to be updated
     * @return The number of rows affected. 
     */
    public int update(List<Map<String, String>> params);

    /**
     * Deletes a list of shards based on shard ids (aka shard name). The list
     * will only contain shard ids.
     * 
     * @param params
     * @return The number of rows affected. 
     */
    public int delete(List<Map<String, String>> params);

    /**
     * Deletes all shards from the shard table.
     */
    public int deleteAll();

    /**
     * List all shards definitions
     * 
     * @returns A list of shards represented by maps of name-value.
     */
    public List<Map<String, String>> list();

}
```

_Note_:  It may be convenient to introduce a call to set the shard
disposition only.  This makes changing shard dispositions simpler for clients.

Here is a simple example of usage.
```
    // Shard management MBean and some data structures.  
    ShardManagementMBean mbean; 
    HashMap<String,String> shard;
    List<Map<String,String>> args = new ArrayList<Map<String,String>>();

    // ...look up mbean and connect. 
         
    // Insert a shard. 
    shard = new HashMap<String,String>();
    shard.put("name", "test");
    shard.put("master", "nyc");
    shard.put("critical", "false");
    args.add(shard);
    mbean.insert(args);
        
    // Update a shard to change the master. 
    args.clear();
    shard.put("master", "sjc");
    args.add(shard);
    mbean.update(args);
        
    // Delete the shard. 
    args.clear();
    shard = new HashMap<String,String>();
    shard.put("name", "test");
    args.add(shard);
    mbean.delete(args);
```

## Trepctl Additions for Shard Processing ##

The trepctl utility will be extended to add support for shard management.  There will be a new "shard" command with the following syntax:

```
trepctl [-service name] shard { -insert | -update | -delete | -list}
```

The -list command lists all shards using a CSV format that looks like the following.  The first line contains column names, while subsequent lines are whitespace-separated values.  Separator characters are space, tab, and comma.  Multiple separator characters in succession are treated as a single separator.

```
name    master        critical
db1       sjc         true
db2       nyc         false
```

The -insert, -update, and -delete options accept the same file type as
input.  When reading input, blank lines and any line that starts with a # are ignored.

You can use these commands to do quick and easy edits on the shard catalog.  The following example shows how to dump the shard catalog, edit it, and reload.

```
bin/trepctl -service test shard -list > shard.map
vi shard.map
bin/trepctl shard -service test -update < shard.map
```

The last command will import the file and process data from it, updating rows that are found in the table and inserting new rows when not found.

## Shard Filter ##

Shards will be processed using a shard filter, which will be implemented as Java class ShardFilter, which implements the Tungsten Filter interface.

The shard filter class has the following properties that may be set in the replicator service properties file:

| **Property** | **Description** |
|:-------------|:----------------|
| autoCreate         | Automatically add new shards to catalog |
| enforceHome        | If true, allow update from master service only; otherwise drop the value |

The shard filter class can be added to any stage of any pipeline.  It will be installed on remote pipelines by default.  Only one such filter per pipeline is supported.  Adding additional filters may cause primary key errors and other problems.

The shard filter reads the shard catalog table when its prepare() method is called.  It stores the shard definition in an in-memory cache.

If the shard filter sees a new shard that is not in the catalog and the autoCreate property is true, it will create a new shard record.  The shard will receive the current service as master.

If the enforceHome property is true, the shard filter handles transactions as follows:

  1. If the filter is running in a local service, all transaction are  accepted.  For example, if the filter is running in local service "sjc" it accepts all shards regardless of the master value.  This means that the "sjc" master forwards all shards it finds and "sjc" local slaves accept all shards.  This behavior is identical to standard master/slave behavior.
  1. If the filter is running in a remote service, there are two cases to consider.
    1. Shard master matches replicator service name.  Transaction is applied.  For example, if a transaction from shard "sjc" is processed by a shard filter in remote process "sjc", it will be applied.  This means that "sjc" updates are propagated from the master.
    1. Shard master does not match replicator service name.  Transaction is dropped.  For example, if a transaction from shard "sjc" is processed by a shard filter in remote process "nyc", it will be dropped.  This means that "nyc" updates are not propagated back from "sjc," thereby avoiding loops.

If enforceHome property is true but autoCreate is false, shards without previously defined catalog entries will be dropped.  There could be a policy for this case.  In some cases one might want to have the replicator fail so the problem would be noticed and fixed.

## Shard Partitioner ##

The ShardListPartitioner class used to separate shards for parallel repliation will be updated to use the new shard catalog table.  The existing shard.list file will no longer be supported.  This unifies parallel replication support with the shard management API.

## Shard Status ##

The current 'trepctl status -name shards' command will be updated to include shard metadata from the shard catalog table as well as dynamic counts from the currently operating pipeline.

This means that all shards will be listed in the status command even if they have not had any transactions since the replicator went online.  Currently the status command only shows shards processed since the pipeline went online.

# Practical Applications of the Shard Management API #

In this final section we consider practical application to the use cases mentioned previously.  For these examples we assume that the shard filter is added to each replicator pipeline with the following property settings:

```
# Configure shard filter for system-of-record enforcement.  Shards must
# be explicitly defined.  Shard masters are enforced. 
replicator.filter.shard=com.continuent.tungsten.enterprise.replicator.filter.ShardFilter
replicator.filter.shard.autoCreate=false
replicator.filter.shard.enforceHome=true
```

We further assume that all shard definitions are defined in a text file called 'shards.txt'.  This could be stored in a spreadsheet as well and dumped to CSV.

## Example 1: Definition of New Customer ##

To define a new customer cust\_c whose master is "nyc," the steps are as follows.
  1. Edit the shards.txt file and add the following line.
```
# name    master        critical    channel
...
cust_c    nyc         false       null
```
  1. Upload the file to all replicator services on sites "sjc" and "nyc".  **Note**:  This can be optimized to load into only remote services.
```
trepctl -host sjc1 -service sjc shard -update < shards.txt
trepctl -host sjc1 -service nyc shard -update < shards.txt
trepctl -host nyc1 -service sjc shard -update < shards.txt
trepctl -host nyc1 -service nyc shard -update < shards.txt
```
  1. Start processing for customer cust\_c on the "nyc" host.  This
can occur as soon as the trepctl commands complete successfully.

## Example 2: Switching Customer Master ##

To switch the master for cust\_c from "nyc" to "sjc" the steps are as follows.

  1. Edit the shards.txt file and edit the following line.
```
# name    master      critical    channel
...
cust_c    sjc         false       null
```
  1. Stop processing on cust\_c.
  1. Issue a trepctl flush command and wait for the sequence number it returns to be committed on sjc.  When the wait command returns all transactions from cust\_c have flushed through both masters.
```
$ trepctl -host nyc -service nyc flush
Master log is synchronized with database at log sequence number: 4313339
$ trepctl -host sjc -service nyc wait -applied 4313339
```
  1. Upload the file to all replicator services on sites "sjc" and "nyc".  **Note**:  This can be optimized as noted before to load into only remote services.
```
trepctl -host sjc1 -service sjc shard -update < shards.txt
trepctl -host sjc1 -service nyc shard -update < shards.txt
trepctl -host nyc1 -service sjc shard -update < shards.txt
trepctl -host nyc1 -service nyc shard -update < shards.txt
```
  1. Start processing for customer cust\_c on the "sjc" host.  This can occur as soon as the trepctl commands complete successfully.

## Example 3: Detect Discrepancies in Rules ##

To check for discrepancies between replicator services, dump the shard entries from each and diff them, as shown below.

```
trepctl -host sjc1 -service sjc shard -list > shard.sjc1.sjc.txt
trepctl -host nyc1 -service sjc shard -list > shard.nyc1.sjc.txt
diff shard.sjc1.sjc.txt shard.nyc1.sjc.txt
```