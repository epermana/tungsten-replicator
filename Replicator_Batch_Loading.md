# Contents #

# Introduction #

Tungsten Replicator normally applies SQL changes to slaves by constructing SQL statements and executing in the exact order that transactions appear in the Tungsten History Log (THL).  This works well for OLTP databases like MySQL, PostgreSQL, Oracle, and MongoDB.  However, it is a poor approach for data warehouses.

Data warehouse products like Vertica or GreenPlum load very slowly through JDBC interfaces (50 times slower or even more compared to MySQL).  Instead, we would really like to use a batch loader.  It turns out that both Vertica and Greenplum support the PostgreSQL COPY command, which allows data to be loaded quickly from CSV.

Batch loading is good for OLTP databases, too.  MySQL's LOAD DATA INFILE can operate 20x faster than SQL inserts in some cases.  This makes it desirable for loading row updates quickly.

The Tungsten [BatchApplier](http://code.google.com/p/tungsten-replicator/source/browse/trunk/replicator/src/java/com/continuent/tungsten/replicator/applier/batch/BatchApplier.java) class is the first implementation of batch loading for Tungsten Replicator.  The rest of this file describes how it works.

# Theory of Operation #

Java class BatchApplier loads data into the slave DBMS using CVS files and LOAD DATA INFILE.  The first implementation works on MySQL and PostgreSQL databases.  Here is the basic algorithm.

While executing within a commit block, we write incoming transactions into open CSV files written by class CsvWriter.   There is one CSV file per database table.  The following sample shows typical contents.
```
"84900","I","986","http://www.continent.com/software","1"
"84901","D","143",null,"2"
"84901","I","143","http://www.microsoft.com","3"
```

Rows start with the Tungsten sequence number (seqno) followed by a transaction code "I" for insert and "D" for delete.  The last value in the row is a row ID that shows the order in which rows were written to the file.  Different update types are handled as follows:

  * Each insert generates a single row containing all values in the row with an "I" opcode.
  * Each delete generates a single row with the key and a "D" opcode.  Non-key fields are null.
  * Each update results in a delete with the row key followed by an insert.
  * Statements are ignored.   If you want DDL you need to put it in yourself.

Tungsten writes each row update into the corresponding CSV file for the SQL.  At commit time the following steps occur.
  1. Flush and close each CSV file.  This ensures that if there is a failure the files are fully visible in storage.
  1. Execute a load script to move the data from CSV into a staging table using a command like COPY or LOAD DATA INFILE.
  1. Execute a merge script to move the data from the staging table to the underlying base table.

Staging tables have a special format that is described in a later section.  Staging tables introduce an extra step but have the advantage that users can apply flexible logic to decide how to load data.  This has applications that go beyond data warehouse loading.  For instance, you can use staging tables to implement conflict resolution rules for multi-master replication.

The main requirement of the load and merge scripts is that they must ensure rows load and that delete and insert operations apply in the correct order.   Tungsten includes load scripts for MySQL and Vertica that do this.

# Batch Applier Setup #

Here is how to set up on MySQL.

1. Enable row replication on the MySQL master using `set global binlog_format=row` or by updating my.cnf.

2. Tweak the wrapper.conf file in the release to enable the correct platform encoding and timezone for the Java VM.  Uncomment the following lines and edit to suit your platform.

```
# You may need to set the Java platform charset to replicate heterogeneously
# from MySQL using row replication.  This should match the default charset
# of your MySQL tables.  Common values are UTF8 and ISO_8859_1.  Many Linux# platforms default to ISO_8859_1 (latin1). 
wrapper.java.additional.4=-Dfile.encoding=UTF8

# To ensure consistent handling of dates in heterogeneous and batch replication
# you should set the JVM timezone explicitly.  Otherwise the JVM will default
# to the platform time, which can result in unpredictable behavior when 
# applying date values to slaves.  GMT is recommended to avoid inconsistencies.
wrapper.java.additional.5=-Duser.timezone=GMT
```

3. Install using the --batch-enable=true option.  Here's a typical installation command.  The options for validation checks are necessary due a problem in the tungsten-installer.

```
tools/tungsten-installer --direct -a \
  --master-host=logos1  \
  --master-user=tungsten  \
  --master-password=secret  \
  --slave-host=logos2 \
  --slave-user=tungsten  \
  --slave-password=secret  \
  --service-name=batch \
  --batch-enabled=true \
  --batch-load-template=mysql \
  --home-directory=/opt/continuent \
  --channels=1 \
  --buffer-size=1000 \
  --mysql-use-bytes-for-string=false \
  --skip-validation-check=MySQLConfigFileCheck \
  --skip-validation-check=MySQLExtractorServerIDCheck \
  --skip-validation-check=MySQLApplierServerIDCheck \
  --property=replicator.filter.pkey.addPkeyToInserts=true \
  --property=replicator.filter.pkey.addColumnsToDeletes=true \
  --svc-parallelization-type=disk --start-and-report 
```

Batch installation creates an applier definition for batch loading that looks like the following.  You can change additional property values by tweaking the static properties file manually or using the --property option.

```
# Batch applier basic configuration information. 
replicator.applier.dbms=com.continuent.tungsten.replicator.applier.batch.SimpleBatchApplier
replicator.applier.dbms.url=jdbc:mysql:thin://${replicator.global.db.host}:${replicator.global.db.port}/tungsten_${service.name}?createDB=true
replicator.applier.dbms.driver=org.drizzle.jdbc.DrizzleDriver
replicator.applier.dbms.user=${replicator.global.db.user}
replicator.applier.dbms.password=${replicator.global.db.password}
replicator.applier.dbms.startupScript=${replicator.home.dir}/samples/scripts/batch/mysql-connect.sql

# Timezone and character set. 
replicator.applier.dbms.timezone=GMT+0:00
replicator.applier.dbms.charset=UTF-8

# Parameters for loading and merging via stage tables. 
replicator.applier.dbms.stageTablePrefix=stage_xxx_
replicator.applier.dbms.stageDirectory=/tmp/staging
replicator.applier.dbms.stageLoadScript=${replicator.home.dir}/samples/scripts/batch/mysql-load.sql
replicator.applier.dbms.stageMergeScript=${replicator.home.dir}/samples/scripts/batch/mysql-merge.sql
replicator.applier.dbms.cleanUpFiles=false
```

Restart the slave replicator and batch loading is enabled.  From there on out you can start to test.

# Staging Table Format #

Staging tables must follow a specific format that mimics the base table to which they apply.  There is one staging table for each base table.

For example, suppose we have a base table created by the following CREATE TABLE command:

```
CREATE TABLE `croc_insertfloat` (
  `id` int(11) NOT NULL,
  `f_data` float DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

To make inserts on this table you must create a staging table that has the same format including column names, types, and order.  Beyond that there are two differences.  First, the table should not have a primary key or you will get primary key violations if there are multiple inserts on the same row.  Second column must contain a row\_id column, which is used to distinguish between multiple inserts.  Here is the corresponding table matching the preceding example:

```
CREATE TABLE `stage_xxx_croc_insertfloat` (
  `tungsten_seqno` int(11) DEFAULT NULL,
  `tungsten_opcode` char(1) DEFAULT NULL,
  `id` int(11) NOT NULL,
  `f_data` float DEFAULT NULL,
  `tungsten_row_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

**Note**:  Staging tables are by default in the same schema as the table they update.  You can put them in a different schema using the stageSchemaPrefix property, which adds a prefix to the base schema name.  If the replicator cannot find a required transfer table, it will fail.

**Another note**:  Staging table columns other than the base table primary key must allow nulls.  Otherwise you will get failures when processing deletes.  For now you should not define primary keys on the staging tables.

# Connect, Copy, and Load Scripts #

The BatchApplier supports three parameterized SQL scripts, which are controlled by the following properties.

| **Property** | **Description** |
|:-------------|:----------------|
| startupScript | Script that executes on login to initialize the session |
| stageLoadScript | Script that loads data at commit time from CSV to the staging table |
| stageMergeScript | Script that merges data at commit from the staging to the base table |

The load and merge scripts accept the following parameters.  Parameters are surrounded by %% symbols, which is ugly but unlikely to be confused with SQL.

| Parameter | Description |
|:----------|:------------|
| %%CSV\_FILE%% | Full path to CVS file |
| %%BASE\_TABLE%% | Fully qualified name of the base table |
| %%BASE\_COLUMNS%% | Comma-separated list of base table columns |
| %%STAGE\_TABLE%% | Fully qualified name of the staging table |
| %%PKEY%%  | Primary key column name |
| %%BASE\_PKEY%% | Fully qualified base table primary key name |
| %%STAGE\_PKEY%% | Fully qualified stage table primary key name |

Load scripts have the following conventional names:  _template_-connect.sql, _template_-load.sql, and _template_-merge.sql.  The template name is given by the tungsten-installer --batch-load-template option.

Here is the connect script for MySQL:
```
# MySQL connection script.  Ensures consistent timezone treatment. 
SET time_zone = '+0:00';
```

Here is the load script for MySQL:
```
# Load script for MySQL.  First command ensures timezone is set to GMT. 
# Note the use of UTF8 charset.  MySQL and the replicator must likewise
# be configured to use UTF8. 
SET time_zone = '+0:00';
LOAD DATA LOCAL INFILE '%%CSV_FILE%%' INTO TABLE %%STAGE_TABLE%% 
  CHARACTER SET utf8 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
```

Here is the merge script for MySQL:
```
# Merge script for MySQL. 
#
# Delete rows.  This query applies all deletes that match, need it or not. 
# The inner join syntax used avoids an expensive scan of the base table 
# by putting it second in the join order. 
DELETE %%BASE_TABLE%% 
  FROM %%STAGE_TABLE%% s
  INNER JOIN %%BASE_TABLE%% 
  ON s.%%PKEY%% = %%BASE_TABLE%%.%%PKEY%% AND s.tungsten_opcode = 'D'

# Insert rows.  This query loads each inserted row provided that the 
# insert is (a) the last insert processed and (b) is not followed by a 
# delete.  The subquery could probably be optimized to a join. 
REPLACE INTO %%BASE_TABLE%%(%%BASE_COLUMNS%%) 
  SELECT %%BASE_COLUMNS%% FROM %%STAGE_TABLE%% AS stage_a
  WHERE tungsten_opcode='I' AND tungsten_row_id IN 
  (SELECT MAX(tungsten_row_id) FROM %%STAGE_TABLE%% GROUP BY %%PKEY%%)
```

Note that it is important to get the delete+insert logic right or you will end up applying data in a way that breaks serialization.  It is also **very** easy to write slow merge queries.  The foregoing queries are OK on smallish data sets but the DELETE operation in particular causes substantial random I/O if the number of deletes is very large.  If you develop new scripts, test them carefully on large data sets.

Load scripts are stored by convention in directory tungsten-replicator/samples/scripts/batch.  You can find scripts for MySQL as well as Vertica there.

# Character Sets #

Character sets are a headache in batch loading because all updates are written and read from CSV files, which can result in invalid transactions along the replication path.  Such problems are very difficult to debug.  Here are some tips to improve chances of happy replicating.

  1. Use UTF8 character sets consistently for all string and text data.
  1. Force Tungsten to convert data to Unicode rather than transferring strings.  I.e., specify `tungsten-installer ... --mysql-use-bytes-for-string=false`.
  1. When starting the replicator for MySQL replication, include the following command in the wrapper.conf file: `wrapper.java.additional.4=-Dfile.encoding=UTF8`.

# Time Zones #

Time zones are another headache when using batch loading.  For best results applications should standardize on a single time zone, preferably UTC, and use this consistently for all data.  To ensure the Java VM outputs time data correctly to CSV files, you must set the JVM time zone to be the same as the standard time zone for your data.  Here is the JVM setting in wrapper.conf:

```
# To ensure consistent handling of dates in heterogeneous and batch replication
# you should set the JVM timezone explicitly.  Otherwise the JVM will default
# to the platform time, which can result in unpredictable behavior when 
# applying date values to slaves.  GMT is recommended to avoid inconsistencies.
wrapper.java.additional.5=-Duser.timezone=GMT
```

In addition, you must ensure that the batch applier

**Note**:  Beware that MySQL has two very similar data types:  timestamp and datetime.   Timestamps are stored in UTC and convert back to local time on display.  Datetimes by contrast do not convert back to local time.  If you mix timezones and use both data types your time values will be inconsistent on loading.

# Testing and Operation #

You can test BatchApplier using the new croc utility from bristlecone.  Croc is documented in bristlecone/doc/CROC.TXT.

You can also test using any load within the capabilities described in the next section.  Remember to run DDL on both master and slave databases.

# Capabilities #

The BatchApplier implementation has the following general capabilities.

  * The BatchApplier should correctly apply any sequence of insert and update equivalently to the normal fully serialized operation with SQL statements.
  * INSERT, UPDATE, and DELETE statements should be handled.
  * The following datatypes are supported:  INT, BIGINT, DATE, TIMESTAMP, VARCHAR, DOUBLE, FLOAT, DECIMAL.
  * Replicator management functions, for example the trepctl, should function exactly as with normal fully serialized replication.
  * Configuration parameters like the global buffer size (replicator.global.buffer.size) behave exactly as before.  In fact, this value should be increased to a very large value for the last stage so that the CSV files contain as many rows as possible at commit time.
  * The replicator will clean up CSV files and temp tables after commit and whenever it goes offline.

# Limitations #

There are a number of limitations to the work done so far.

  * The applier is still in active development.  Testing is limited to specific use cases.
  * Not all datatypes are supported or even tested.
  * Parallel replication has not been tested.
  * When using staged loading with transfer tables, note that all tables must have a single key, that key must have the same name, and its value must be an INT.

These limitations will be corrected in future builds.