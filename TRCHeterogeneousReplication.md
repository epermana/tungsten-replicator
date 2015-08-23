
Up to [the Tungsten Replicator Cookbook](TungstenReplicatorCookbook.md)

# Heterogeneous Replication #

## PostgreSQL to PostgreSQL and MySQL Replication ##

The following examples show how to install a PostgreSQL -> PostgreSQL & MySQL topology on a single host.

### Install a prototype PostgreSQL logical replicator (master) ###

Prepare the master PostgreSQL instance:

  1. Download [Slony](http://slony.info/downloads/), extract it and build it. After doing that, there will be a slonik executable available, which will you need to specify via "--postgresql-slonik" property (as shown below).
  1. Specify tables to replicate in "--postgresql-tables".

Here's a full example:

```
./tools/tungsten-installer --master-slave -a --cluster-hosts=127.0.0.1 \
--user=postgres \
--master-host=127.0.0.1 \
--datasource-type=postgresql \
--datasource-port=54321 \
--postgresql-dbname=pgbench \
--home-directory=/opt/pg2pg/tungsten/S \
--datasource-user=postgres \
--datasource-password=secret \
--service-name=fromslony \
--rmi-port=12020 \
--thl-port=12021 \
--postgresql-slonik=/opt/pg2pg/slony1-2.0.6/src/slonik/slonik \
--postgresql-tables=public.tt1,public.t \
--start-and-report
```

Note: the latter command will drop any existing Slony triggers and objects and then recreate them.

### Install a PostgreSQL applier replicator (slave) ###

A slave is installed on a different PostgreSQL instance:

```
./tools/tungsten-installer --master-slave -a --cluster-hosts=127.0.0.1 \
--user=postgres \
--master-host=127.0.0.1 \
--datasource-type=postgresql \
--datasource-port=54323 \
--postgresql-dbname=pgbench \
--home-directory=/opt/pg2pg/tungsten/P \
--datasource-user=postgres \
--datasource-password=secret \
--service-name=fromslony \
--rmi-port=12022 \
--master-thl-port=12021 \
--thl-port=12023 \
--start-and-report
```

If replicating from a PostgreSQLSlonyExtractor enabled master, slave needs to have tables already created, as DDL is not replicated with this method.

### Install a MySQL applier replicator (slave) ###

Note the filters we enable for MySQL slave to be able to apply PostgreSQL origin events:

```
./tools/tungsten-installer --master-slave -a --cluster-hosts=127.0.0.1 \
--user=tungsten \
--master-host=127.0.0.1 \
--datasource-type=mysql \
--datasource-port=12002 \
--mysql-enable-ansiquotes=true \
--mysql-enable-noonlykeywords=true \
--home-directory=/opt/pg2mysql/M \
--datasource-user=tungsten \
--datasource-password=secret \
--service-name=fromslony \
--rmi-port=12024 \
--master-thl-port=12021 \
--thl-port=12025 \
--start-and-report
```

## MySQL to PostgreSQL and Oracle Replication ##

It is recommended to use ROW replication on the MySQL master, because pure SQL statements can have different dialect and fail on other type DBMS.

### Install a MySQL Master ###

If you have ENUM types in the database, make sure you enable "--mysql-enable-enumtostring" option:

```
./tools/tungsten-installer --master-slave -a --cluster-hosts=127.0.0.1 \
--master-host=127.0.0.1 \
--user=tungsten \
--home-directory=/opt/mysql2others/M \
--datasource-port=12001 \
--datasource-user=tungsten \
--datasource-password=secret \
--service-name=frommysql \
--rmi-port=20000 \
--thl-port=12112 \
--mysql-enable-enumtostring=true \
--mysql-use-bytes-for-string=false \
--skip-validation-check=MySQLNoMySQLReplicationCheck \
--start-and-report
```

Note: "--mysql-use-bytes-for-string=false" option ensures that character fields are transferred in a way that other DBMS types understand.

### Install a PostgreSQL Slave ###

If you're planning to replicate some known DDL statements, you can enable pgddl.js filter with option "--postgresql-enable-mysql2pgddl" and extend it to include the statements you need to support:

```
./tools/tungsten-installer --master-slave -a --cluster-hosts=127.0.0.1 \
--user=postgres \
--master-host=127.0.0.1 \
--datasource-type=postgresql \
--datasource-port=54324 \
--postgresql-dbname=postgres \
--home-directory=/opt/mysql2pg/P \
--datasource-user=postgres \
--datasource-password=secret \
--service-name=frommysql \
--rmi-port=10012 \
--master-thl-port=12112 \
--thl-port=12111 \
--postgresql-enable-mysql2pgddl=true \
--start-and-report
```

### Install an Oracle Slave ###

Before installing Oracle slave, prepare a tungsten\_frommysql user in Oracle identified by the same password, you are providing via "--datasource-password" property. Internal Tungsten tables are created in this user, as well as all transactions arrive here.

Below is an example of a simple (i.e. no special filtering) Oracle slave installation:

```
./tools/tungsten-installer --master-slave -a --cluster-hosts=localhost \
--user=oracle \
--master-host=localhost \
--home-directory=/opt/mysql2oracle/O \
--datasource-type=oracle \
--datasource-oracle-service=ORCL \
--datasource-user=tungsten \
--datasource-password=secret \
--service-name=frommysql \
--rmi-port=10000 \
--master-thl-port=12112 \
--thl-port=12110 \
--start-and-report
```

## MySQL to MongoDB Replication ##

The following examples show how to install MySQL to MongoDB replication.

### Install a MySQL Master ###

Install a MySQL master replicator.  MySQL must use ROW replication, so you must set binlog\_format=row before enabling replication.  You must enable the colnames and pkey filter so that the MongoDB can generate column names and also generate MongoDB indexes on primary key values.

```
tools/tungsten-installer --master-slave -a \
  --datasource-type=mysql \
  --master-host=logos1  \
  --datasource-user=tungsten  \
  --datasource-password=secret  \
  --service-name=mongodb \
  --home-directory=/opt/continuent \
  --cluster-hosts=logos1 \
  --mysql-use-bytes-for-string=false \
  --svc-extractor-filters=colnames,pkey \
  --java-file-encoding=UTF8 \
  --svc-parallelization-type=none --start-and-report
```

### Install a MongoDB Slave ###

Install MongoDB 1.8.3 and then install a MongoDB slave.  Other MongoDB versions may work as well.

```
tools/tungsten-installer --master-slave -a \
  --datasource-type=mongodb \
  --master-host=logos1  \
  --datasource-user=tungsten  \
  --datasource-password=secret  \
  --service-name=mongodb \
  --home-directory=/opt/continuent \
  --cluster-hosts=logos2 \
  --java-file-encoding=UTF8 \
  --skip-validation-check=InstallerMasterSlaveCheck \
  --svc-parallelization-type=none --start-and-report
```

MySQL tables will be materialized as collections in MongoDB.  Columns convert to BSON properties.  SQL primary keys will be converted to (composite) indexes on collections.

Up to [the Tungsten Replicator Cookbook](TungstenReplicatorCookbook.md)