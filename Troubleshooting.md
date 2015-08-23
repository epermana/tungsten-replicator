

# Trouble shooting #

Tungsten is a complex piece of software. Although we are striving to make the installation process as easy as possible, there are many places where something can go wrong. This page lists the most frequent cases.

## Requirements ##

### Operating system ###

  * **ssh**. The operating system user must be able to connect to the other nodes in the cluster to perform the installation. Such user must connect without need for a password, which you can do either by setting a SSH key without a passphrase, or by using ssh-agent. Either way, if the installer complains about something related to SSH, you should make sure that the user can run a command remotely.
  * **sudo**. This is a tricky requirement. You will need sudo access only if you plan to use a backup method that requires root access (such as xtrabackup). If you are using mysqldump, you don't need it, and you can force installation by skipping this validation (`--skip-validation-check=SudoCheck`). If you want to use this feature, though, you need to enable sudo broadly for this user, making sure that _requiretty_ is not set in /etc/sudoers;
  * the user must be able to **read MySQL binary logs**. The simplest way to meet this requirement is making sure that the MySQL data directory is group readable, and assigning the 'mysql' group to the installation user.

### MySQL setup ###

MySQL is supported from version 4.1 to 5.5 (we will test with new versions as soon as they become stable). In order to install Tungsten, you need to make sure of the following:
  * Binary logs must be enabled;
  * Either the binary log name is 'mysql-bin' or you need to specify  the name pattern;
  * Every server must have a unique server ID;
  * MySQL replication must not be active (unless you are installing with the `--direct` method);
  * the MySQL user that Tungsten will use for replication must have all privileges with grants option;

If any of the above conditions is not met, the installer will complain and refuse to install.

### MySQL defaults ###

The installer assumes that the data directory is in _/var/lib/mysql_. That'e the place where the data directory is found after a RPM installation. If your logs are in a different path, you can use the `--datasource-log-directory` to tell the installer where it is.

The other notable default value is the database server port (3306). A non-standard port can be announced to the installer using `--datasource-port`.


### Java ###

Java must be installed for Tungsten to run. It requires Java 1.6, regardless of the package. Tungsten runs fine with the official Sun/Oracle Java, both JRE or JDK, and with openJDK.

### Ruby ###

Ruby 1.8.5 is required. Installing it is straightforward. Depending on the flavor of your OS, either `apt-get ruby` or `yum ruby` will get the job done. If you see errors complaining about missing libraries, probably you need to install a package called `ruby-libs`.
If you decide to install Ruby on your own, by compiling from source, be aware that one of the libraries (`openssl.rb`) is compiled only if you have installed a package called `openssl-devel`. If that package is missing, Ruby compiles without errors, but then the Tungsten installer will complain that Ruby can't find a given library.

## Installation ##
### Problems with ./configure ###
If you are having problems with the _./configure_, then you are using an outdated version of Tungsten Replicator. As of version 2.0.4, _./configure_ and _./configure-service_ are deprecated. Instead, you should use **./tools/tungsten-installer** and **./tools/configure-service**. The documentation covers their usage in detail.

### Problems with a **second** installation ###
If you have installed Tungsten, and then you want to reinstall with the same servers, either because there was an error, or you have realized that you would like to install with different parameters, you must make sure that the previous installation is removed.
Therefore, before installing Tungsten again, do the following in all the servers where Tungsten was installed:

  * stop the replicator service. (`$tungsten_base/tungsten/tungsten-replicator/bin/replicator stop`).
  * make sure the replicator is stopped, using the 'ps' command. If for any reason the replicator does not stop regularly (it may happen if there is a bug or the wrong procedure was used before), you must force it to stop using 'kill'.
  * remove all the THL files (usually in $tungsten\_home/thl/svc\_name)
  * drop the tungsten\_svc\_name schema from MySQL
  * Either run 'reset master', or use the appropriate options when installing (`--master-log-file` and `--master-log-port`).

## Operation ##

### Changes are not appearing on the slave ###
  * Run `tungsten-replicator/bin/trepctl services` to make sure all replication services are `ONLINE`
  * If a service is not `ONLINE`, run `tungsten-replicator/bin/trepctl -service service_name status` to get more details
  * Review the `tungsten-replicator/log/user.log` and `tungsten-replicator/log/trepsvc.log` for additional information about the error
  * Once you have resolved the error, restart replication by running `tungsten-replicator/bin/trepctl -service service_name online`
  * Use the trepctl command to monitor replication status and resolve any other issues

### Help! My dates are getting corrupted! ###

Time zones are a headache.  For best results applications should standardize on a single time zone, preferably UTC, and use this consistently for servers in all locations.  To ensure the Java VM handles time data correctly, you must set the **JVM time zone** to be the same as the standard time zone for your data.  Here is the JVM setting in wrapper.conf:

```
# To ensure consistent handling of dates in heterogeneous and batch replication
# you should set the JVM timezone explicitly.  Otherwise the JVM will default
# to the platform time, which can result in unpredictable behavior when 
# applying date values to slaves.  GMT is recommended to avoid inconsistencies.
wrapper.java.additional.5=-Duser.timezone=GMT
```

Restart the replicator after making this change.

**Note**:  Beware that MySQL has two very similar data types:  timestamp and datetime.   Timestamps are stored in UTC and convert back to local time on display.  Datetimes by contrast do not convert back to local time.  If you mix timezones and use both data types your time values will be inconsistent on loading.

### Help! International characters are getting corrupted! ###

If you see cases where character strings are corrupted between master and slave, you may need to set the JVM platform character set using the --java-file-encoding option at installation time.  Here's an example for heterogeneous replication, which also needs to transfer strings.

tools/tungsten-installer ... --java-file-encoding=UTF8 --mysql-use-bytes-for-string=false ...

If you have already installed, you can update the option directly in wrapper.conf.  Look for the following, change appropriately, and restart the replicator to take effect.

```
# You may need to set the Java platform charset to replicate heterogeneously
# from MySQL using row replication.  This should match the default charset
# of your MySQL tables.  Common values are UTF8 and ISO_8859_1.  Many Linux
# platforms default to ISO_8859_1 (latin1).
wrapper.java.additional.4=-Dfile.encoding=UTF8
```

This problem occurs most commonly when using row replication and heterogenous transfer but it can also occur in MySQL to MySQL cases.  In that case make sure you use --mysql-use-bytes-for-string=true, which is the default.

### `Event application failed: seqno=3 fragno=0 message=java.sql.SQLException: Statement failed on slave but succeeded on master` ###
The key to resolving this issue is to examine the failed transaction and then take action to resolve the issue and then restart replication.

  * Search for `com.continuent.tungsten.replicator.applier.ApplierException` in the `tungsten-replicator/log/trepsvc.log` file.  The stacktrace displayed in the log should give you some information about the error encountered while applying the transaction.
  * If you need additional information about the transaction, use the thl utility to extract the event data.  A transaction can include multiple fragments, make sure that you look at the fragment where the error occurred. [TungstenReplicatorCookbook#Inspect\_Transaction\_History\_Logs](TungstenReplicatorCookbook#Inspect_Transaction_History_Logs.md)
```
./tungsten-replicator/bin/thl -service service_name list -seqno 3
```
```
SEQ# = 3 / FRAG# = 0 (last frag)
- TIME = 2011-09-14 09:06:42.0
- EPOCH# = 0
- EVENTID = mysql-bin.000002:0000000000000592;11
- SOURCEID = db1
- METADATA = [mysql_server_id=101;service=service_name;shard=test]
- TYPE = com.continuent.tungsten.replicator.event.ReplDBMSEvent
- SQL(0) = SET INSERT_ID = 2
- OPTIONS = [##charset = ISO8859_1, autocommit = 1, sql_auto_is_null = 1, foreign_key_checks = 1, unique_checks = 1, sql_mode = '', character_set_client = 8, collation_connection = 8, collation_server = 8]
- SCHEMA = 
- SQL(1) = insert into test.names (name) values ('Robert')
```
  * Take action to resolve the error.  Sometimes the proper resolution is to manually run the statements in the transaction.  If you take this course of action, make sure to run all of the fragments in the transaction.  You will be able to skip the whole transaction when taking the replication service online.
  * Resume replication of the database.  If you manually ran the statements in the transaction, skip it while resuming replication.  [TRCAdministration#Suspend\_and\_resume\_replication](TRCAdministration#Suspend_and_resume_replication.md)

#### Statement looks like `UPDATE tungsten_alpha.heartbeat SET source_tstamp= '2011-09-15 14:36:18', salt= 1, name= 'MASTER_ONLINE'  WHERE id= 1 /* ___SERVICE___ = [alpha] */` ####

If the replicator fails while applying a statement like this, you should check that the service name is the same on the master and the slave.  If they differ, you should remove the replicator on the slave and reinstall using the service name from the master.

### `Failing statement : INSERT INTO test.people (  ,  ,  ,  ,  ,  ,  )  VALUES (  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  )` ###

This error occurs when using row replication and the table is not present on the slave.  Verify that the table exists and put the replication service online.  If the problem persists, check `tungsten-replicator/log/trepsvc.log` for problems accessing the database system.

### `There was an error parsing the config file: expected next name, value pair in object at '},"datasources": {"d'! (RuntimeError)` ###

There is a problem with the `configs/tungsten.cfg` file.  If you manually modified it, check for any trailing commas or JSON parsing errors.  Try running `tools/configure --output-config` after making modifications to see if the config file can be properly parsed.

### `Unable to determine the current config for tungsten@db1:/opt/continuent/tungsten` ###

There is a problem accessing the host or reading the `configs/tungsten.cfg` file.  If you are able to successfully SSH to the machine, or if it is the localhost, check the validity of the config file. [#There\_was\_an\_error\_parsing\_the\_config\_file:\_expected\_next\_name,](#There_was_an_error_parsing_the_config_file:_expected_next_name,.md)

Short link to this page: [http://bit.ly/trtshoot](http://bit.ly/trtshoot)