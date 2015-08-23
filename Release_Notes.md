
# Tungsten Replicator Release Notes #

## Introduction ##

This page contains release notes for Tungsten Replicator open source releases.

## Tungsten Replicator 2.2.0 (23 December 2013) ##

For 2.2.0 release notes, see [2.2.0 Release Notes](http://docs.continuent.com/tungsten-replicator-2.2/release-notes-2-2-0.html)

For documentation, see the new documentation posted [here](http://docs.continuent.com/tungsten-replicator-2.2/index.html)

## Tungsten Replicator 2.1.2-44 Maintenance Release (27 November 2013) ##

This is a maintenance release that provides fixes for some specific issues related to Oracle and reset operations within a multimaster deployment.

**Bug Fixes**

  * **Oracle Replication**
    * `DATETIME` values would not be correctly translated to the Oracle `DATE` type during replication. _Issues_: [704](https://code.google.com/p/tungsten-replicator/issues/detail?id=704)
  * **Core Replicator**
    * Running **trepctl reset** on a service deployed in an multimaster (all master) configuration would not correctly remove the schema from the database. _Issues_: [758](https://code.google.com/p/tungsten-replicator/issues/detail?id=758)

## Tungsten Replicator 2.1.2 ##

Tungsten Replicator 2.1.2 is a bugfix release that fixes specific bugs affected by `DATETIME`, that were identified in 2.1.1. Tungsten Replicator therefore includes these fixes in addition to all the features and functionality that originally appeared in Tungsten Replicator 2.1.1.

<font color='red'>Behavior Changes</font> 

The following changes have been made to Tungsten Replicator and may affect existing scripts and integration tools. Any scripts or environment which make use of these tools should check and update for the new configuration:

  * There has been a significant change in the THL format used for storing timestamp information, as a result of changes to the processing required for supporting MySQL 5.6.When upgrading to the latest version, slaves must be upgraded before the master to ensure that they are able to cope with the timestamp storage format change before the information is written to the THL on the master.Once slaves have been updated, the masters can be updated.
  * The internal CRC check used by MySQL 5.6 was incompatible with the binary log extractor. _Issues_: [461](https://code.google.com/p/tungsten-replicator/issues/detail?id=461)
  * When operating using ROW-based replication, `DATETIME` columns would show time differences during a daylight savings time (DST) change.In addition, timestamps may have replicated with different values if the master and slave were configured with different timezones.
> > To address these issue the configuration of Tungsten Replicator installations must be updated.
> > The issue occurs because a valid date/time replicated to the slave creates a an invalid time due to daylight savings time when applied to the MySQL database. In the process of trying to fix the incorrect date/time, the time is updated before it is applied into the database. This is due to inconsistent timezone configurations in MySQL and the system timezone used by the Java VM used by Tungsten Replicator.The inconsistent date/time information has been fixed, but requires configuration changes to ensure that information is not replicated incorrectly:
    * The MySQL and Java JVM timezone configurations must be the same. Ideally, the system timezone should also be the same. Differences in the timezone configuration of Java and MySQL will cause differences in the stored `DATETIME` values on the slave.To set the timezone for MySQL:
```
    mysql> SET GLOBAL time_zone = timezone;
```
To set the timezone for your operating system, on Ubuntu/Debian, update the value in the file `/etc/timzezone`. On CentOS/RedHat, create a symbolic link from the right file within `/usr/share/zoneinfo` to `/etc/localtime`.To set the timezone for the JVM for Tungsten Replicator, use the `--timezone` configuration option to **tpm** to update your configuration.
    * Timezone configurations on all hosts within a cluster must be set to the same timezone to prevent values drifting between hosts.
    * Individual changes to the timezone configuration of the system or MySQL may introduce differences.
    * Values inserted during daylight savings time changes should be correct during a time change; ensure your timezone is configured properly and your servers are synchronized.In addition to the configuration requirements, the format of the THL files has been changed. When upgrading:
    * Slaves should be upgraded first, this will ensure that the slaves can read the new THL format
    * Once the slaves have been upgraded, upgrade the master. Once upgraded and reconfigured, the information output by **thl** may indicate a different date/time value than either extracted from the master, or that will be applied to the slave, due to the way the information is stored within the THL. Time differences may show a multiple-hour difference compared to the applied value. _Issues_: [542](https://code.google.com/p/tungsten-replicator/issues/detail?id=542)
  * Support for MySQL 5.6 row-based replication has been added to Tungsten Replicator. Replication is now supported for the following MySQL releases:
    * MySQL 5.0, 5.1, 5.5, 5.6
    * Percona 5.5
    * MariaDB 5.5\_Issues_: [558](https://code.google.com/p/tungsten-replicator/issues/detail?id=558)_

**Improvements, new features and functionality**

  * **Installation and Deployment**
    * The replicator has been updated to support secure communication between replicators and between administrative tools (**trepctl**) and the replicator processes. Security operates at two separate levels within the configuration:
      * SSL support for the THL transfer between replicator instances. This feature enables full SSL certificate encrypted transmission of the THL data.
      * SSL and authentication support for administration. Administration, both local and remote, through **trepctl** and other tools can be encrypted and secured through an authorized user for the Java RMI channel.Security can be enabled for either or both components, and existing installations can be updated to use the secure installation.Secure installation is only supported when using **tpm** to perform installations and updates. _Issues_: [508](https://code.google.com/p/tungsten-replicator/issues/detail?id=508), [638](https://code.google.com/p/tungsten-replicator/issues/detail?id=638), [656](https://code.google.com/p/tungsten-replicator/issues/detail?id=656), [664](https://code.google.com/p/tungsten-replicator/issues/detail?id=664), [665](https://code.google.com/p/tungsten-replicator/issues/detail?id=665), [666](https://code.google.com/p/tungsten-replicator/issues/detail?id=666), [667](https://code.google.com/p/tungsten-replicator/issues/detail?id=667), [668](https://code.google.com/p/tungsten-replicator/issues/detail?id=668)
    * To provide better interaction with complex environments, including those created by the Cookbook system, **tpm** supports the setting of additional `PATH` locations to be searched before the standard path. The `--preferred-path` option specifies one or more directories to be prepended to the `PATH` by Tungsten Replicator, including backup/restore tools, Ruby, Java and other utilities. _Issues_: [582](https://code.google.com/p/tungsten-replicator/issues/detail?id=582); _Tags_: tpm:preferred-path
    * The **tpm** command has been updated to support the option `--timezone`, which sets the corresponding JVM timezones. _Issues_: [596](https://code.google.com/p/tungsten-replicator/issues/detail?id=596)
    * The **tpm** tool has been updated to make the installation of different complex topologies easier. The improvements involve the following major changes:
      * The `--topology` option now supports explicit settings for `all-masters` (multi-master topology), `fan-in`, and `star`.
      * A list of services can be supplied where multiple services are created, for example during multi-master, fan-in, or star configurations. The list of services to be created is specified by the `--master-services` option, accepting a comma-separated list of service names. For example, when creating a multi-master configuration with four hosts, `--master-services=alpha,beta,gamma,delta` will create four services for the corresponding list of masters, i.e. `--masters=host1,host2,host3,host4` would create a service on `host1` called `alpha`, on `host2` called `beta` and so on.
      * The specification of hosts has been simplified:
        * The list of master hosts (comma-separated) can be specified using the `--masters` or `--master` option.
        * The list of slave hosts (comma-separated) can be specified using the `--slaves` option.
        * The list of members (i.e. masters and slaves) can continue to be specified using the `--members` option.**tpm** will calculate the appropriate list of slaves, masters, and services from this information and create the corresponding configurations during deployment. _Issues_: [623](https://code.google.com/p/tungsten-replicator/issues/detail?id=623)
    * Use of **tungsten-installer** for performing installations will be deprecated in a future release. The **tpm** command will be used instead. **tpm** has been updated to support reading, and updating from an existing configuration, migrating a service installed using **tungsten-installer** to use **tpm**. _Issues_: [641](https://code.google.com/p/tungsten-replicator/issues/detail?id=641), [669](https://code.google.com/p/tungsten-replicator/issues/detail?id=669)
  * **Command-line Tools**
    * The **trepctl** has been improved to support the output of a list of connected slaves (clients) to a running master service. _Issues_: [635](https://code.google.com/p/tungsten-replicator/issues/detail?id=635)
  * **Cookbook Utility**
    * Cookbook scripts have been updated to use the **tpm** command for installation of a cluster, in place of **tungsten-installer**. _Issues_: [620](https://code.google.com/p/tungsten-replicator/issues/detail?id=620)
    * The cookbook toolset has been expanded to include explicit tools that integrate more effectively with MySQL Sandbox. Specifically:
      * **deploy\_to\_sandboxes** has been renamed to **deploy\_sandboxes**.
      * A **remove\_sandboxes** command has been added to remove existing sandboxes. A corresponding tool, **restore\_user\_values** saves back the contents of the `USER_VALUES.sh` and `USER_VALUES.local.sh` scripts to versions that do not use the sandbox infrastructure.
      * The **db\_use** tool has been created and provides a direct route into a MySQL command-line tool for a specific host. Use the `-h` command-line option to specify the host. For example:
```
      shell> db_use -h host1
```
> > > _Issues_: [640](https://code.google.com/p/tungsten-replicator/issues/detail?id=640)
    * When uninstalling an installation using the cookbook through **clear\_cluster**, the process would run sequentially. The process has now been updated to run concurrently. _Issues_: [653](https://code.google.com/p/tungsten-replicator/issues/detail?id=653)
    * The cookbook has been updated to check and warn if an installation is started and `USE_TPM` has not been enabled. A warning is generated in the event of executing the installer without the `USE_TPM` variable being enabled and set to 1. The warning and delay can be disabled by setting `INSTALLATION_DELAY=0` before executing the cookbook installation scripts. _Issues_: [675](https://code.google.com/p/tungsten-replicator/issues/detail?id=675)
  * **Backup and Restore**
    * The backup scripts that work in combination with **trepctl** have been documented. The documentation includes details on how the backup process works, and how custom backup scripts can be created, including sample scripts. _Issues_: [590](https://code.google.com/p/tungsten-replicator/issues/detail?id=590)
    * During a restore operation, a replicator is not automatically placed into _`ONLINE`_ mode, instead the replicator is placed in _`OFFLINE`_ mode. _Issues_: [609](https://code.google.com/p/tungsten-replicator/issues/detail?id=609)
  * **Oracle Replication**
    * The `CDCMetadataFilter` and **ddlscan** has been updated to support placing the CDC columns at the start of a row, instead of at the end of the row. _Issues_: [628](https://code.google.com/p/tungsten-replicator/issues/detail?id=628)

**Bug Fixes**

  * **Installation and Deployment**
    * **tungsten-installer** would fail ungracefully when required applications were missing. _Issues_: [280](https://code.google.com/p/tungsten-replicator/issues/detail?id=280)
    * When running the **configure-service** tool from an incorrect location could result in the error message Unable to run configure-service because this directory has not been setup . Advisory text has now been added to the error message. _Issues_: [288](https://code.google.com/p/tungsten-replicator/issues/detail?id=288)
    * Tungsten Replicator has been updated to compile correctly against Java 1.7. _Issues_: [619](https://code.google.com/p/tungsten-replicator/issues/detail?id=619)
    * Installation of a fan-in cluster using **tungsten-installer** would fail when the fan-in slave is not last host in the list of hosts. _Issues_: [652](https://code.google.com/p/tungsten-replicator/issues/detail?id=652)
  * **Command-line Tools**
    * When using the **thl** command with the `-headers` option, the entire THL content, including the full event and transaction data would be deserialized, which was inefficient. _Issues_: [645](https://code.google.com/p/tungsten-replicator/issues/detail?id=645)
    * The inline help within **trepctl** contained some duplication and inconsistencies. _Issues_: [646](https://code.google.com/p/tungsten-replicator/issues/detail?id=646)
  * **Cookbook Utility**
    * The **test\_cluster** script within the cookbook did not check for the online state within deployed services. _Issues_: [626](https://code.google.com/p/tungsten-replicator/issues/detail?id=626)
    * Cookbook deployments used relayed binary logs in place of directly reading binary logs. _Issues_: [627](https://code.google.com/p/tungsten-replicator/issues/detail?id=627)
    * The cookbook tool would fail to delete and clear an existing installation if `LOCAL_USER_VALUES` had been used during installation. _Issues_: [637](https://code.google.com/p/tungsten-replicator/issues/detail?id=637)
    * When configuring a new installation, the IP detection procedure would fail when a host has multiple IP addresses configured. _Issues_: [654](https://code.google.com/p/tungsten-replicator/issues/detail?id=654)
  * **Backup and Restore**
    * When starting a backup through **trepctl** when the backup directory was empty (for example, after a move), the process would fail instead of recreating the required directory structure and contents. _Issues_: [552](https://code.google.com/p/tungsten-replicator/issues/detail?id=552)
    * When performing a backup on the master and restoring this backup to a slave, the backup contents would be inconsistent with the replication state, causing intermittent replication errors. This was due to the asynchronous nature of the backup process. The backup process has been updated to correctly clear the status information during restore. _Issues_: [556](https://code.google.com/p/tungsten-replicator/issues/detail?id=556)
    * Due to changes in the operating method, Tungsten Replicator was not compatible with Percona Xtrabackup 2.1. _Issues_: [629](https://code.google.com/p/tungsten-replicator/issues/detail?id=629)
  * **Oracle Replication**
    * Running the **setupCDC.sh** command without specifying a correct service name would cause ongoing failures in the replication setup. The tool has been updated to report an error if the service name has not been set. _Issues_: [622](https://code.google.com/p/tungsten-replicator/issues/detail?id=622)
  * **Core Replicator**
    * Due to a minor change in the binlog format used by MySQL, session variables were not replicated correctly for ROW-based events. _Issues_: [624](https://code.google.com/p/tungsten-replicator/issues/detail?id=624)
    * In some situations, parallel replication threads responsible for reading THL fail silently, resulting in badly lagging channels. The issue was due to a fault in the way failing channels and pipelines were identified and reported. _Issues_: [636](https://code.google.com/p/tungsten-replicator/issues/detail?id=636)
    * When replicating ROW-based events, null values for keys could incorrectly be handled. _Issues_: [659](https://code.google.com/p/tungsten-replicator/issues/detail?id=659)
    * When using ROW-based replication, `DATETIME` values of 0 (zero) would cause a ClassCastException when applied to a dataserver. _Issues_: [679](https://code.google.com/p/tungsten-replicator/issues/detail?id=679)
    * When using ROW-based replication, `TIME` values with a microsecond component would be incorrectly replicated, removing the microsecond component. _Issues_: [681](https://code.google.com/p/tungsten-replicator/issues/detail?id=681)
  * **Filters**
    * The `RenameFilter` file has been documented. _Issues_: [612](https://code.google.com/p/tungsten-replicator/issues/detail?id=612)
  * **Unclassified**
    * Due to a previous change to allow selection of `SQL_MODE` variable settings to be ignored when processing events. The supported setting of enabling `ALLOW_INVALID_DATES` would be identified incorrectly as `INVALID_DATES`. _Issues_: [642](https://code.google.com/p/tungsten-replicator/issues/detail?id=642)


## Tungsten Replicator 2.1.1 ##

<font color='red'>Tungsten Replicator 2.1.1 was recalled and is no longer available.</font>

## Tungsten Replicator 2.1.0 ##

This release is the first to include both extraction and applier support for Oracle within the open-source product. Other new features include an updated and re-certified MongoDB applier; improved support for replication to Vertica 6; improvements to the command-line tools to provide JSON output to allow for easier third-party processing; significant improvements and expansion of the Cookbook system for building different cluster topologies (see enclosed README); support for archive hosts; a new filter for renaming schemas, databases and tables, and other improvements and bug fixes.

Upgrading Tungsten Replicator can be achieved using the **./tools/update** command. For more information on upgrading, see [Upgrading Tungsten Replicator](https://docs.continuent.com/wiki/display/TEDOC/Upgrading+Tungsten+Replicator).

The following improvements and fixes are available in Tungsten Replicator 2.1.0.

<font color='red'>Behavior Changes</font>

The following changes have been made to Tungsten Replicator and may affect existing scripts and integration tools. Any scripts or environment which make use of these tools should check and update for the new configuration:

  * Tungsten uses comments appended to statements to mark the replication service name. This allows Tungsten replicators to recognize the origin of statements and prevent statement loops. This feature is only required in multi-master replication topologies where statements are replicated into another master again then extracted from the log. It is not required in simple master/slave topologies or those master/master topologies in which updates are not logged into the database log. However, in star topologies the comment information is required to prevent duplication or re-application of statements. It is also required in master/master topologies where updates are logged when applied to another master.Comments are now disabled by default. Cookbook templates for star topologies have been updated to enable this feature by default. This option should be enabled when creating a star topology, or when advised to do so. However, care should be taken to ensure that character set definitions for their table data, definitions and environment matches to prevent issues with the addition of comments to existing statements. Issue [547](https://code.google.com/p/tungsten-replicator/issues/detail?id=547)
  * In previous releases, a restore operation using **trepctl** would automatically set the replicator _`ONLINE`_ once the restore operation had been completed. In Tungsten Replicator 2.1.0, the replicator will remain in the offline state after a restore operation. Issue [450](https://code.google.com/p/tungsten-replicator/issues/detail?id=450)

The following improvements, new features, and functionality, are available in this release:

  * **Command-line Tools and Scripts**
    * Added a **thl** mode that outputs headers on a single line and supports JSON based output. The new command-line option is `-headers`, which outputs only the header and metadata information (without the SQL or row data) on a single line. The additional `-json` alters the output formatting to use the JSON format, with a single record for each event sequence fragment. Issue [576](https://code.google.com/p/tungsten-replicator/issues/detail?id=576)
    * The **trepctl** command has been updated to support the `-json` command-line option for status and service output to make it easier to parse the output of information from the tool. Issue [499](https://code.google.com/p/tungsten-replicator/issues/detail?id=499)
    * The **tpm** command can be used instead of **tungsten-installer** for installing and configuring services.Cookbook recipes still use the **tungsten-replicator** command, and **tpm** is not yet certified for general use. Issue [501](https://code.google.com/p/tungsten-replicator/issues/detail?id=501)
  * **Cookbook Utility**
    * A script has been added to the Cookbook system that automatically starts a load on the replication service using Bristlecone. The new script is `cookbook/load_data.sh` in the Tungsten Replicator directory. Issue [483](https://code.google.com/p/tungsten-replicator/issues/detail?id=483)
    * Deploy MySQL sandboxes in several hosts for Tungsten installations. Issue [485](https://code.google.com/p/tungsten-replicator/issues/detail?id=485)
    * Shortcuts to the Tungsten Replicator tools have been added to the Cookbook directory, allowing for easier execution of selected tools and operations, such as the replicator and configuration viewing and editing. Issue [525](https://code.google.com/p/tungsten-replicator/issues/detail?id=525)
    * The Cookbook **cleanup** script now provides a number of environment variables that can be used to configure and select which items should be cleaned during the scripts execution. Issue [533](https://code.google.com/p/tungsten-replicator/issues/detail?id=533)
    * The Tungsten Replicator Cookbook recipes have been improved so that they use the current known topology of the cluster and the names have been simplified to be easier to use and identify. Issue [541](https://code.google.com/p/tungsten-replicator/issues/detail?id=541)
    * Added a dry-run option to cookbook installation scripts to enable the operations that will be performed to be shown. Issue [527](https://code.google.com/p/tungsten-replicator/issues/detail?id=527)
  * **Core Replicator**
    * The parallel apply functionality has been improved to support sharding the information by sequence number within the THL. This can be used to improve initial loads of THL data to a new hosts, particularly in heterogeneous deployments. Issue [478](https://code.google.com/p/tungsten-replicator/issues/detail?id=478)
    * Very rarely, the CRC for a THL event can becomed corrupted, causing loading and parsing of the THL and replication to fail. Parsing of the THL would fail, making it impossible to identify the location of the failure and the event causing the problem. Within the **thl** command, the `-no-checksum` option now enables you to view the THL ignoring checksum errors if they exist.Additionally, the **trepctl** command has been updated to support the `-no-checksum` option. This switches off both CRC checking when reading the file, and generation of the CRC when replication data is written to the THL. In the event of a CRC failure, the THL should be examined and the CRC checksum switched off if the event is safe to be processed. Once processed, replication should be stopped and restarted without the `-no-checksum` option to enable checksum on THL events.
    * Replicators can now be configured as _archive_ hosts. These download the THL but do not apply the THL to the database. Archive hosts can be used to act as a record of the THL which can help during recovery. Issue [549](https://code.google.com/p/tungsten-replicator/issues/detail?id=549)
    * The **xtrabackup** command used within the replicator restore procedure can now restore files directly to the MySQL data directory. Issues [568](https://code.google.com/p/tungsten-replicator/issues/detail?id=568), [487](https://code.google.com/p/tungsten-replicator/issues/detail?id=487)
    * A general purposes Ruby library has been provided that supports basic API operations into the core Tungsten Replicator. General Tungsten scripting Ruby library. More information can currently be located within the `cluster-home/lib/ruby/tungsten/README` file within the distribution. Issue [569](https://code.google.com/p/tungsten-replicator/issues/detail?id=569)
  * **Filters**
    * A new filter has been added, `RenameFilter`, which allows for easy renaming of schemas, tables and columns for ROW-based replication. Issue [464](https://code.google.com/p/tungsten-replicator/issues/detail?id=464)
  * **Oracle Replication**
    * Support has been added to allow master extraction of the Oracle data for use in replication. This enables heterogeneous replication both to and from Oracle databases. Issue [551](https://code.google.com/p/tungsten-replicator/issues/detail?id=551)
    * The **ddlscan** tool includes a number of new features to make operations easier when scanning the Oracle database. These options include the ability to import additional templates, user-defined template options, interface for determining reserved words and integrated use of RenameFilter CSV file. Issue [462](https://code.google.com/p/tungsten-replicator/issues/detail?id=462)
    * Some sample scripts for easing the provisioning of MySQL databases to Oracle. Issue [585](https://code.google.com/p/tungsten-replicator/issues/detail?id=585)
  * **Other Issues**
    * The **ddlscan** command templates can be used to generate schema and staging table DDL for Vertica. Issue [467](https://code.google.com/p/tungsten-replicator/issues/detail?id=467)
    * Backup storage agent moves files instead of copying them. This reduces the space requirement during backup. Issue [262](https://code.google.com/p/tungsten-replicator/issues/detail?id=262)
    * During installation and configuration, the THL port numbers for all services are compared to ensure that there are no duplicate port specifications on each host. A warning is generated during configuration. This affects **tpm** only. Issue [337](https://code.google.com/p/tungsten-replicator/issues/detail?id=337)
    * Concurrent garbage collection support has been added to the running replicator service. This can be configured by using the `--java-enable-concurrent-gc=true` option to the **tungsten-installer** or **configure** commands. Issue [412](https://code.google.com/p/tungsten-replicator/issues/detail?id=412)
    * The port number in a THL host configuration (`--master-thl-host`) specification can be specified on the command line using the `host:port` format. Issue [472](https://code.google.com/p/tungsten-replicator/issues/detail?id=472)
    * The replicator service can now be started without automatically going into the online state. To start, or restart, the replicator but start in the offline mode, use the `-offline` option:
```
shell> replicator start -offline
```

> > Issue [553](https://code.google.com/p/tungsten-replicator/issues/detail?id=553)
    * A `--service-name` argument has been added to **tpm** to specify the service name to override the default service name. Issue [563](https://code.google.com/p/tungsten-replicator/issues/detail?id=563)
    * A _`dump`_ command has been added as an alias for the _`reverse`_ command to the **tpm** command. Issue [564](https://code.google.com/p/tungsten-replicator/issues/detail?id=564)

**Bug Fixes**

The following fixes and updates are included in this release:

  * **Cookbook Utility**
    * When installing a cluster using cookbook, the cookbook tools could not be used from the installed directory, only from the staging directory. directoryIssue [532](https://code.google.com/p/tungsten-replicator/issues/detail?id=532)
    * The cookbook test script would not detect missing services, reporting a success even if no services were identified. Issue [540](https://code.google.com/p/tungsten-replicator/issues/detail?id=540)
    * The `cookbook/load_data` script would send data to the master as configured in the initial installation, instead of the current master within the replication service. Issue [544](https://code.google.com/p/tungsten-replicator/issues/detail?id=544)
    * Backup and restore operations would fail when using **xtrabackup** within the cookbook. The cookbook installer scripts now accept the option `--datasource-boot-script` to point to the required boot script for restarting the database server. Issue [578](https://code.google.com/p/tungsten-replicator/issues/detail?id=578)
    * The cookbook scripts did not work correctly with MySQL 5.6 due to the change in command-line password acceptance. Issue [581](https://code.google.com/p/tungsten-replicator/issues/detail?id=581)
    * The cookbook recipes for multi-master installation had hard coded values for the `BINLOG_DIRECTORY`. Issue [480](https://code.google.com/p/tungsten-replicator/issues/detail?id=480)
  * **Filters**
    * The `Eventmetadata` filter would assign an empty string shard ID instead of `#UNKNOWN`, which would cause parallel apply failures. Issue [477](https://code.google.com/p/tungsten-replicator/issues/detail?id=477)
    * The `ReplDBMSFilteredEvent` filter would force commits on batch processing which would severely slow batch loading of data. Issue [592](https://code.google.com/p/tungsten-replicator/issues/detail?id=592)
  * **Installation and Configuration**
    * Previously, a replicator configuration installed with the `start-and-report` option would return a 0 exit code, even if the replicator fails to start properly. The **tungsten-installer** now returns a non-zero result if the replicator fails to start after installation. Issue [277](https://code.google.com/p/tungsten-replicator/issues/detail?id=277)
    * The installer will now report an error if InnoDB has not been enabled on a MySQL server, is not the default storage engine, and if the required tables for Tungsten were not created as InnoDB tables. Issue [279](https://code.google.com/p/tungsten-replicator/issues/detail?id=279)
    * The installer has been updated to warn about unsafe values for the MySQL `innodb_flush_log_at_trx_commit` configuration value. Issue [482](https://code.google.com/p/tungsten-replicator/issues/detail?id=482)
    * The MySQL-to-MongoDB replication configuration has been fixed. A fault in the definition of the required filters and parallel apply functionality caused replication to MongoDB to fail on startup. Issue [572](https://code.google.com/p/tungsten-replicator/issues/detail?id=572)
  * **Oracle Replication**
    * The **setupCDC.sh** script could not update an existing configuration when there were schema changes. The command has been updated to update the information during changes. Issue [522](https://code.google.com/p/tungsten-replicator/issues/detail?id=522)
    * The `updateCDC.sh` script would generate an nondescript error if the CDC tables did not exist. Issue [560](https://code.google.com/p/tungsten-replicator/issues/detail?id=560)
  * **Other Fixes**
    * Tungsten replicator did not work correctly with Vertica 6 because the wrong commands and configuration settings were being used. Issue [463](https://code.google.com/p/tungsten-replicator/issues/detail?id=463)
    * Slave replicator would stall intermittently when parallel apply was enabled. Issue [466](https://code.google.com/p/tungsten-replicator/issues/detail?id=466)
    * On systems with slow filesystems, the replicator would generate error messages about needing to read more bytes than available from the filesystem. The identification and message have been changed. Issue [470](https://code.google.com/p/tungsten-replicator/issues/detail?id=470)
    * The replicator would fail to reduce the number of tasks properly when parallel apply is running and applier has been running for a long time. Issue [476](https://code.google.com/p/tungsten-replicator/issues/detail?id=476)
    * Replicator becomes unresponsive after `OutOfMemoryError` but does not indicate the error in the log. Issue [484](https://code.google.com/p/tungsten-replicator/issues/detail?id=484)
    * Taking a slave replicator offline immediately after process restart would result in the slave channel positions resetting to an earlier sequence no. Issue [493](https://code.google.com/p/tungsten-replicator/issues/detail?id=493)
    * Slave applier can fail to log error when DBMS fails due to exception in cleanupIssue [537](https://code.google.com/p/tungsten-replicator/issues/detail?id=537)
    * A master failure would cause partial commits on the slave configured with single channel parallel apply. Issue [546](https://code.google.com/p/tungsten-replicator/issues/detail?id=546)
    * The replicator would fail to go online if last transaction was at the end of a THL log file. Issue [570](https://code.google.com/p/tungsten-replicator/issues/detail?id=570)
    * The replicator does not correctly pick up the preferred master role when replicating from a Tungsten cluster after a switch. Issue [605](https://code.google.com/p/tungsten-replicator/issues/detail?id=605)
    * Multi table delete would not be detected correctly by the replicator and would lead to data inconsistencies. Issue [399](https://code.google.com/p/tungsten-replicator/issues/detail?id=399)
    * Documentation has been added about the EPOCH number stored in the THL. See [EPOCH Description](https://docs.continuent.com/wiki/display/TEDOC/Transaction+History+Log#TransactionHistoryLog-terminologythleventepoch)Issue [444](https://code.google.com/p/tungsten-replicator/issues/detail?id=444)
    * The Java Service Wrapper has been modified to make it easier to define additional parameters. Issue [447](https://code.google.com/p/tungsten-replicator/issues/detail?id=447)
    * On certain operation system configurations some installation commands could fail, such as **scp**. The installation scripts have been updated to handle differences in operating system versions and support. Issue [455](https://code.google.com/p/tungsten-replicator/issues/detail?id=455)
    * The **replicator** command does not return the correct result code when running the command as a different user. Issue [469](https://code.google.com/p/tungsten-replicator/issues/detail?id=469)
    * The replicator installation would break when working in `--direct` mode. Issue [486](https://code.google.com/p/tungsten-replicator/issues/detail?id=486)
    * Using the **deploy\_to\_sandboxes** command within the cookbook, the data directory would not be updated in the `USER_VALUES` option. Issue [488](https://code.google.com/p/tungsten-replicator/issues/detail?id=488)
    * Using row-based replication, updates of binary data did not behave correctly for short values. Issue [489](https://code.google.com/p/tungsten-replicator/issues/detail?id=489)
    * The internal JMX management port would use a random port instead of a fixed port, making it difficult to configure firewall values accordingly. This has been updated to use the configured management (RMI) port (default 10000) + 1. Issue [490](https://code.google.com/p/tungsten-replicator/issues/detail?id=490)
    * The replicator would generate invalid SQL on a `DROP TEMPORARY TABLE` statement if the `Preventative` filter is configured to operate twice on the replication stream. Issue [491](https://code.google.com/p/tungsten-replicator/issues/detail?id=491)
    * The replicator startup script would return 0 even when the replicator process is not running. Issue [531](https://code.google.com/p/tungsten-replicator/issues/detail?id=531)
    * The CDC generating filter has been updated to support a different starting point for the CDC sequence number (`replicator.filter.customcdc.sequenceBeginning` property). The CDC has also been updated to support using a single schema for the CDC information using the `replicator.filter.customcdc.toSingleSchema` property. Issue [534](https://code.google.com/p/tungsten-replicator/issues/detail?id=534)
    * The cookbook installer for fan-in topology did not install slave services correctly. Issue [538](https://code.google.com/p/tungsten-replicator/issues/detail?id=538)
    * The `utilities.sh` script gets duplicate slaves when installing a multi-master topology. Issue [539](https://code.google.com/p/tungsten-replicator/issues/detail?id=539)
    * The exception message generated when a statement fails to be applied would get truncated to 1000 characters making it difficult to identify the statement contents. Issue [543](https://code.google.com/p/tungsten-replicator/issues/detail?id=543)
    * The load data script `concurrent_evaluator.pl` would stop only one instance instead of all of them during executing. Issue [545](https://code.google.com/p/tungsten-replicator/issues/detail?id=545)
    * Replication between MySQL and MongoDB could fail with a Null Pointer Exception. Issue [548](https://code.google.com/p/tungsten-replicator/issues/detail?id=548)
    * MySQL `TEXT` tables would not be replicated to MongoDB. Issue [567](https://code.google.com/p/tungsten-replicator/issues/detail?id=567)
    * Backup using **trepctl** could fail. Issue [577](https://code.google.com/p/tungsten-replicator/issues/detail?id=577)
    * Removing a service using the Oracle extractor would fail. Issue [586](https://code.google.com/p/tungsten-replicator/issues/detail?id=586)
    * When replicating a statement that included the `UNHEX` function within MySQL, an extra space would be added to the data. Issue [601](https://code.google.com/p/tungsten-replicator/issues/detail?id=601)
    * The Slave replicator would time out very slowly on connection to master when network interface is down, which could cause problems during auto failover. Issue [603](https://code.google.com/p/tungsten-replicator/issues/detail?id=603)

## Tungsten Replicator 2.0.7 ##

Tungsten 2.0.7 enables new MySQL versions, provides better support for multi-master and parallel replication, and improves setup of advanced topologies.  In the MySQL area, we have added the ability to replicate from MySQL into Amazon RDS instances as initial certification of MySQL 5.6.  There are several important new features for multi-master replication, including better control of specific schemas that should be replicated between masters, as well as the ability for slave replicators switch automatically between a group of replicators in a Tungsten cluster.  There are a number of bug fixes to help with parallel replication, including a new relative latency setting that helps detect when replication is idle or stalled.  New ./ddlscan utility allows flexible schema transformations, checks and custom content rendering based on templates.  Finally, we have improved cookbook recipes so that it is easier to set up interesting topologies.

Tungsten 2.0.7 has no known upgrade incompatibilities with Tungsten 2.0.6.  Use the tools/update command as described in the [product documentation](https://docs.continuent.com/wiki/display/TEDOC/Upgrading+replicators).

[Issue 258](https://code.google.com/p/tungsten-replicator/issues/detail?id=258) - Statement transactions lack SQL parsing metadata for filtering after they are logged

[Issue 270](https://code.google.com/p/tungsten-replicator/issues/detail?id=270) - Problems running installer on Solaris

[Issue 304](https://code.google.com/p/tungsten-replicator/issues/detail?id=304) - Replicator hangs on connection to dead network interface

[Issue 323](https://code.google.com/p/tungsten-replicator/issues/detail?id=323) - UPDATEs and DELETEs on a slave which update zero rows should fail

[Issue 370](https://code.google.com/p/tungsten-replicator/issues/detail?id=370) - Add support for CVS batch loading using external utilities like Calpont cpimport

[Issue 371](https://code.google.com/p/tungsten-replicator/issues/detail?id=371) - Parallel replication may lock up intermittently when Amazon instances are too small

[Issue 374](https://code.google.com/p/tungsten-replicator/issues/detail?id=374) - Implement purge operation

[Issue 382](https://code.google.com/p/tungsten-replicator/issues/detail?id=382) - Replication commits partial transaction following application error or OutOfMemory condition

[Issue 386](https://code.google.com/p/tungsten-replicator/issues/detail?id=386) - Add support for fast batch loading using direct inserts and minimal staging table for deletes

[Issue 396](https://code.google.com/p/tungsten-replicator/issues/detail?id=396) - Add option to make replicators read preferentially from a slave rather than a master

[Issue 397](https://code.google.com/p/tungsten-replicator/issues/detail?id=397) - Replicator has problems finding filtered events in log, leading to slave restart failures

[Issue 398](https://code.google.com/p/tungsten-replicator/issues/detail?id=398) - options for mysqldump backup are non-transactional safe

[Issue 401](https://code.google.com/p/tungsten-replicator/issues/detail?id=401) - Channel assigments for round-robin selection are not cleared when replicator goes offline cleanly

[Issue 405](https://code.google.com/p/tungsten-replicator/issues/detail?id=405) - Add Tungsten Replicator Cookbook to the standard tarball

[Issue 408](https://code.google.com/p/tungsten-replicator/issues/detail?id=408) - Incorrect configuration of THL parameter causes potentially large memory usage

[Issue 411](https://code.google.com/p/tungsten-replicator/issues/detail?id=411) - tools/configure fails if the $CONTINUENT\_ROOT/tungsten directory already exists and is empty

[Issue 414](https://code.google.com/p/tungsten-replicator/issues/detail?id=414) - Parallel apply may stop replicating databases if channel number is adjusted after unclean restart

[Issue 416](https://code.google.com/p/tungsten-replicator/issues/detail?id=416) - Slave replicator stalls when parallel replication is enabled with on-disk queues

[Issue 417](https://code.google.com/p/tungsten-replicator/issues/detail?id=417) - Table rename filter with regex support

[Issue 421](https://code.google.com/p/tungsten-replicator/issues/detail?id=421) - Add filter to convert MySQL zero dates to null value

[Issue 422](https://code.google.com/p/tungsten-replicator/issues/detail?id=422) - Omit installation of parallel queues when 'none' is the replication type

[Issue 424](https://code.google.com/p/tungsten-replicator/issues/detail?id=424) - Filter to generate change rows for incoming transactions

[Issue 425](https://code.google.com/p/tungsten-replicator/issues/detail?id=425) - Enable Tungsten replication into Amazon RDS slave

[Issue 426](https://code.google.com/p/tungsten-replicator/issues/detail?id=426) - Add relative latency to enable users to tell when replicator is stalled or slave may be out of date

[Issue 430](https://code.google.com/p/tungsten-replicator/issues/detail?id=430) - Position reported as -1

[Issue 431](https://code.google.com/p/tungsten-replicator/issues/detail?id=431) - Implement ./ddlscan utility for flexible schema transformations

[Issue 433](https://code.google.com/p/tungsten-replicator/issues/detail?id=433) - NULL values ini extract\_timestamp or update\_timestamp cause NPE

[Issue 434](https://code.google.com/p/tungsten-replicator/issues/detail?id=434) - The xtrabackup-incremental script fails if $CONTINUENT\_ROOT/backups/xtrabackup doesn't exist

[Issue 440](https://code.google.com/p/tungsten-replicator/issues/detail?id=440) - tungsten does not read variables with underscores

[Issue 442](https://code.google.com/p/tungsten-replicator/issues/detail?id=442) - BidiRemoteSlaveFilter fails with NullPointerException on filtered events

[Issue 443](https://code.google.com/p/tungsten-replicator/issues/detail?id=443) - shardfilter does not fail replication on wrong shard

[Issue 445](https://code.google.com/p/tungsten-replicator/issues/detail?id=445) - Tungsten installation fails with MySQL 5.6

[Issue 446](https://code.google.com/p/tungsten-replicator/issues/detail?id=446) - Tungsten installer MySQL 5.6 checksum awareness

[Issue 448](https://code.google.com/p/tungsten-replicator/issues/detail?id=448) - The xtrabackup methods ignore innodb\_data\_home\_dir and innodb\_log\_group\_home\_dir

[Issue 449](https://code.google.com/p/tungsten-replicator/issues/detail?id=449) - SetToStringFilter to convert SET to a VARCHAR list

[Issue 456](https://code.google.com/p/tungsten-replicator/issues/detail?id=456) - Reduce the output of tools/configure and tools/configure-service

[Issue 457](https://code.google.com/p/tungsten-replicator/issues/detail?id=457) - Consistency checking fails on keyword-named columns

## Tungsten Replicator 2.0.6 ##

This release includes the first working implementation of batch loading for data warehouses, focusing on Vertica.  It also includes substantial improvements to parallel replication including fixes for bugs that caused replicator freezes as well as problems with serialization in large data sets.  There are minor improvements to enhance multi-master replication as well as a number of general fixes that improve performance and robustness of the replicator as a whole, especially when processing large transactions.  Finally, this release adds support for MySQL to Oracle as well as MySQL to PostgreSQL replication.

You should be able to upgrade from Tungsten Replicator 2.0.5 using the tools/update command.  See the official documentation at https://docs.continuent.com/wiki/display/TEDOC/Tungsten+Documentation+Home, for instructions on upgrade.  Starting with this and future releases, replicator documentation is shifting to a new home on the Continuent website.  It is our goal to provide very complete documentation of replicator operation.

[Issue 8](https://code.google.com/p/tungsten-replicator/issues/detail?id=8) - Race condition when a DDL statement is immediately followed by a DML statement on the same object   Durability

[Issue 39](https://code.google.com/p/tungsten-replicator/issues/detail?id=39) - Auto-commit DDL statements break block commit   Durability

[Issue 51](https://code.google.com/p/tungsten-replicator/issues/detail?id=51) - PostgreSQL Slony Log Extractor: POC

[Issue 90](https://code.google.com/p/tungsten-replicator/issues/detail?id=90) - waitForAppliedSequenceNumber can return before the sequence number event was actually committed

[Issue 129](https://code.google.com/p/tungsten-replicator/issues/detail?id=129) - Certify MySQL->PostgreSQL support

[Issue 168](https://code.google.com/p/tungsten-replicator/issues/detail?id=168) - MySQL extractor is slow due to large number of calls to slow disk metadata commands

[Issue 172](https://code.google.com/p/tungsten-replicator/issues/detail?id=172) - './trepctl online -from-event' is dangerous

[Issue 181](https://code.google.com/p/tungsten-replicator/issues/detail?id=181) - Parallel replication does not use all channels

[Issue 183](https://code.google.com/p/tungsten-replicator/issues/detail?id=183) - Clean up code, unit tests, and build scripts to speed up builds and eliminate warnings and/or hung builds

[Issue 238](https://code.google.com/p/tungsten-replicator/issues/detail?id=238) - Implement an applier for Vertica data warehouse

[Issue 256](https://code.google.com/p/tungsten-replicator/issues/detail?id=256) - Master replicator unable to connect to MariaDB 5.2.9 after installation

[Issue 291](https://code.google.com/p/tungsten-replicator/issues/detail?id=291) - DROP TABLE loops in multi-master environment on MYSQL 5.5

[Issue 299](https://code.google.com/p/tungsten-replicator/issues/detail?id=299) - Installer support for AWS EC2 Linux

[Issue 305](https://code.google.com/p/tungsten-replicator/issues/detail?id=305) - Add installation support for logging slave updates

[Issue 308](https://code.google.com/p/tungsten-replicator/issues/detail?id=308) - Tungsten generates warning and skips prefetch on rows that contain MySQL "zero" dates of the form '0000-00-00 00:00:00'

[Issue 317](https://code.google.com/p/tungsten-replicator/issues/detail?id=317) - Filters should not drop parts of fragmented events, and even less the last fragment

[Issue 328](https://code.google.com/p/tungsten-replicator/issues/detail?id=328) - When reconnecting, slaves do not close sockets for connection to master if the master goes away/offline, leading to file descriptors leak.

[Issue 330](https://code.google.com/p/tungsten-replicator/issues/detail?id=330) - configure\_deployment\_handler.rb uses -C option for rsync, **.so files fail to be copied during installation**

[Issue 333](https://code.google.com/p/tungsten-replicator/issues/detail?id=333) - Tungsten does not correctly compute restart point after restore of hot backup on slave with parallel apply enabled

[Issue 334](https://code.google.com/p/tungsten-replicator/issues/detail?id=334) - Unable to restore the same backup to a slave and then a slave of a slave

[Issue 336](https://code.google.com/p/tungsten-replicator/issues/detail?id=336) - EnumToString filter URL is not properly configured with MySQL schema name

[Issue 339](https://code.google.com/p/tungsten-replicator/issues/detail?id=339) - In row replication, NULL blobs are badly handled and can lead to 'java.sql.SQLException: You need to set exactly X parameters on the prepared statement' when applied on slave

[Issue 343](https://code.google.com/p/tungsten-replicator/issues/detail?id=343) - Remove FSM (Finite State Machine) library source code and use JAR instead

[Issue 346](https://code.google.com/p/tungsten-replicator/issues/detail?id=346) - Add support for round-robin THL connection using a list of THL URLs instead of a single THL

[Issue 355](https://code.google.com/p/tungsten-replicator/issues/detail?id=355) - Oracle UPDATEs that change no columns lead to OptimizeUpdatesFilter removing too much

[Issue 359](https://code.google.com/p/tungsten-replicator/issues/detail?id=359) - Clean-up : remove classes in Commons project that are not needed by Tungsten Replicator

[Issue 360](https://code.google.com/p/tungsten-replicator/issues/detail?id=360) - Pipeline locks up when attempting to serialize #UNKOWN shard using disk parallel queue

[Issue 362](https://code.google.com/p/tungsten-replicator/issues/detail?id=362) - Installer support for round-robin THL connection list

[Issue 364](https://code.google.com/p/tungsten-replicator/issues/detail?id=364) - Option for applier to use column names from the underlying THL event

[Issue 368](https://code.google.com/p/tungsten-replicator/issues/detail?id=368) - tungsten-installer ability to install master extracting from a remote DBMS

[Issue 372](https://code.google.com/p/tungsten-replicator/issues/detail?id=372) - Add Oracle event ID type

[Issue 373](https://code.google.com/p/tungsten-replicator/issues/detail?id=373) - trepctl online -from-event does not work from slave

[Issue 376](https://code.google.com/p/tungsten-replicator/issues/detail?id=376) - OpenReplicatorManager doesn't have correct Method Descriptions

[Issue 377](https://code.google.com/p/tungsten-replicator/issues/detail?id=377) - Tungsten replicator generates a large number of build warnings

[Issue 378](https://code.google.com/p/tungsten-replicator/issues/detail?id=378) - Parallel replication locks up when waiting for #UNKOWN transaction to commit

[Issue 380](https://code.google.com/p/tungsten-replicator/issues/detail?id=380) - JMX configuration file misses the database port in its "resourceJdbcUrl"

[Issue 381](https://code.google.com/p/tungsten-replicator/issues/detail?id=381) - Timestamp value of 0 fails to replicate on MySQL when using row replication.

[Issue 383](https://code.google.com/p/tungsten-replicator/issues/detail?id=383) - Add support for an incremental xtrabackup method

[Issue 384](https://code.google.com/p/tungsten-replicator/issues/detail?id=384) - Tungsten generates invalid SQL when updating DROP TABLE statements to prevent replication loops

[Issue 385](https://code.google.com/p/tungsten-replicator/issues/detail?id=385) - Protobuf fails to deserialize events that are too large (more than 64MB)

[Issue 387](https://code.google.com/p/tungsten-replicator/issues/detail?id=387) - Replication has excessive latency in recent versions

[Issue 389](https://code.google.com/p/tungsten-replicator/issues/detail?id=389) - Replicator default heap size is too low

[Issue 390](https://code.google.com/p/tungsten-replicator/issues/detail?id=390) - replicator does not commit large RBR transactions   Usability

[Issue 392](https://code.google.com/p/tungsten-replicator/issues/detail?id=392) - Replicator hangs on large transactions with multiple fragments

[Issue 393](https://code.google.com/p/tungsten-replicator/issues/detail?id=393) - installer accepts invalid option --log-slave instead of --log-slave-updates

[Issue 394](https://code.google.com/p/tungsten-replicator/issues/detail?id=394) - Rename Tungsten commons library and Eclipse projects

## Tungsten Replicator 2.0.5 ##

This release improves overall replication capabilities by fixing a number of bugs that affected stability of replication in production environments and increases robustness of parallel replication, which now clocks in at 4-5 times MySQL native replication performance.  It also includes robust installation commands for MongoDB and Oracle slaves.  There is a working prefetch applier available.  Finally, a draft batch load capability is available.  (See [Issue 238](https://code.google.com/p/tungsten-replicator/issues/detail?id=238) for more information; Vertica will be fully supported in the next replicator release.)

Unlike previous releases there should be minimal issues with upgrade.  Tungsten 2.0.5 should be fully interoperable with Tungsten 2.0.4.  Note that there may be occasional changes in options used by tungsten-installer. Check 'tungsten-installer --help-all -a' for currently supported settings.

[Issue 17](https://code.google.com/p/tungsten-replicator/issues/detail?id=17) - `./trepctl stop` doesn't stop the Replicator process

[Issue 67](https://code.google.com/p/tungsten-replicator/issues/detail?id=67) - tungsten-installer does not detect already installed instances

[Issue 73](https://code.google.com/p/tungsten-replicator/issues/detail?id=73) - trepctl status does not show the host name and RMI port

[Issue 74](https://code.google.com/p/tungsten-replicator/issues/detail?id=74) - Null point exception when thl is used on an empty file after installing in --direct mode

[Issue 88](https://code.google.com/p/tungsten-replicator/issues/detail?id=88) - trepctl services does not display the latest sequence number with parallel replication

[Issue 147](https://code.google.com/p/tungsten-replicator/issues/detail?id=147) - Enable THL to regenerate itself after an error

[Issue 148](https://code.google.com/p/tungsten-replicator/issues/detail?id=148) - Enable backwards propagation of purge points

[Issue 149](https://code.google.com/p/tungsten-replicator/issues/detail?id=149) - Enable round-robin assignment of shards to channels.

[Issue 152](https://code.google.com/p/tungsten-replicator/issues/detail?id=152) - Support direct extract of MySQL binlog across network

[Issue 153](https://code.google.com/p/tungsten-replicator/issues/detail?id=153) - Improve usability of message logs.

[Issue 174](https://code.google.com/p/tungsten-replicator/issues/detail?id=174) - `./trepctl online -seqno X` leads to deferred offline request

[Issue 186](https://code.google.com/p/tungsten-replicator/issues/detail?id=186) - Installation fails on master only.

[Issue 189](https://code.google.com/p/tungsten-replicator/issues/detail?id=189) - date 0000:00:00 does not replicate in ROW binlog format correctly.

[Issue 200](https://code.google.com/p/tungsten-replicator/issues/detail?id=200) - Tungsten Replicator removes log files before their contents are processed

[Issue 201](https://code.google.com/p/tungsten-replicator/issues/detail?id=201) - Purging all records from log using 'thl purge' hangs replication service

[Issue 210](https://code.google.com/p/tungsten-replicator/issues/detail?id=210) - tungsten installer does not recognize "~" as $HOME

[Issue 217](https://code.google.com/p/tungsten-replicator/issues/detail?id=217) - Regression: Installation fails with error concerning backup methods

[Issue 218](https://code.google.com/p/tungsten-replicator/issues/detail?id=218) - Extractor error during flush JMX call causes replicator to hang

[Issue 220](https://code.google.com/p/tungsten-replicator/issues/detail?id=220) - tungsten-installer --property can't insert an empty property

[Issue 221](https://code.google.com/p/tungsten-replicator/issues/detail?id=221) - thl list assumes 0 as lower seqno in every file

[Issue 226](https://code.google.com/p/tungsten-replicator/issues/detail?id=226) - Add 'version' to trepctl help

[Issue 229](https://code.google.com/p/tungsten-replicator/issues/detail?id=229) - Implement installation support for MySQL to MongoDB replication

[Issue 231](https://code.google.com/p/tungsten-replicator/issues/detail?id=231) - Installation does not support log fsync settings that are fully crash-safe

[Issue 233](https://code.google.com/p/tungsten-replicator/issues/detail?id=233) - Offline operation is very slow on slaves running parallel apply

[Issue 235](https://code.google.com/p/tungsten-replicator/issues/detail?id=235) - MySQL event extraction fails intermittently when using relay log downloading

[Issue 239](https://code.google.com/p/tungsten-replicator/issues/detail?id=239) - Improve DatabaseTransformFilter in order to handle more than one regular expression

[Issue 240](https://code.google.com/p/tungsten-replicator/issues/detail?id=240) - Installer should check replicator JMX ports are not boundIssue

[Issue 241](https://code.google.com/p/tungsten-replicator/issues/detail?id=241) - Develop generic infrastructure for testing replication within and across DBMS types

[Issue 245](https://code.google.com/p/tungsten-replicator/issues/detail?id=245) - TableMapLogEvent are badly extracted for table with large number of columns

[Issue 248](https://code.google.com/p/tungsten-replicator/issues/detail?id=248) - OptimizeUpdatesFilter can fail with NPEIssue 249 - Docs: add `which` to prerequisites

[Issue 251](https://code.google.com/p/tungsten-replicator/issues/detail?id=251) - Slave restart may fail if the last applied transaction was from a filtered event

[Issue 252](https://code.google.com/p/tungsten-replicator/issues/detail?id=252) - CatalogManager does not correctly set the session log level

[Issue 254](https://code.google.com/p/tungsten-replicator/issues/detail?id=254) - ReplicateFilter gets NullPointerException if a filter string is omitted

[Issue 255](https://code.google.com/p/tungsten-replicator/issues/detail?id=255) - Add a new applier that can be used to prefetch data into database cache

[Issue 257](https://code.google.com/p/tungsten-replicator/issues/detail?id=257) - Replicator may fail to process binlog files that are more than 2 GB large

[Issue 259](https://code.google.com/p/tungsten-replicator/issues/detail?id=259) - Direct pipeline start-up after clearing logs fails

[Issue 260](https://code.google.com/p/tungsten-replicator/issues/detail?id=260) - Building with OpenJDK 7 Fails in DiagnosticWizard.java (sun.management.ManagementFactory is not public…)

[Issue 265](https://code.google.com/p/tungsten-replicator/issues/detail?id=265) - Requirements are not complete (rsync missing)

[Issue 267](https://code.google.com/p/tungsten-replicator/issues/detail?id=267) - MySQL extraction fails on binlog file open when using using relay log downloading

[Issue 269](https://code.google.com/p/tungsten-replicator/issues/detail?id=269) - THLStorageCheck breaks multiple master installation

[Issue 272](https://code.google.com/p/tungsten-replicator/issues/detail?id=272) - Infinite loop in options parsing in `configure-service`

[Issue 290](https://code.google.com/p/tungsten-replicator/issues/detail?id=290) - Replicator fails to delete temp files on slaves when processing LOAD DATA LOCAL INFILE

[Issue 295](https://code.google.com/p/tungsten-replicator/issues/detail?id=295) - Using Load Data Infile into temporary table breaks replication

[Issue 297](https://code.google.com/p/tungsten-replicator/issues/detail?id=297) - Enable slave replicators to restart from a specific event ID on the master


## Tungsten Replicator 2.0.4 ##

This release contains greatly improved parallel replication capabilities including high-performance on-disk queues, a production quality single-command install, and new features to support multi-master replication using the system-of-record model.  In addition, there are numerous bug fixes.

This release requires careful attention to upgrade due to the following changes:

  1. **Installation procedures** have changed substantially.  The old tungsten.cfg is not compatible with the new tungsten.cfg file format, which is JSON-based. The new installation uses **./tools/tungsten-installer**, which is also a validation tool.
  1. Installing a direct slave (useful for parallel replication) is simpler and straightforward. Taking over from MySQL native replication and handing back over to the old slave is also easy.
  1. The **trep\_commit\_seqno** table includes additional columns.  The table can be upgraded by running ``ALTER TABLE `trep_commit_seqno` ADD COLUMN `shard_id` VARCHAR(128) NULL DEFAULT NULL; ALTER TABLE `trep_commit_seqno` ADD COLUMN `extract_timestamp` TIMESTAMP NULL DEFAULT NULL;``
  1. There is a new **trep\_shard** table that can be created by running ``CREATE TABLE `trep_shard` ( `name` varchar(128) NOT NULL DEFAULT '', `master` varchar(128) DEFAULT NULL, `critical` tinyint(4) DEFAULT NULL, PRIMARY KEY (`name`) ) ENGINE=InnoDB DEFAULT CHARSET=utf8``
  1. The replicator logs are now integrated by a **user.log**, a more concise and user friendly log.
  1. The installation tools can handle all values in the properties file. In addition to being a single-command installation, the procedure can also customize the replicator properties from the start.

Logs and other data structures should be backwards compatible.  However, due to the nature of these changes we recommend for the safest upgrade that you stop traffic on the master server, quiesce replication fully, delete existing THL logs as well as tungsten\_svcname databases, then restart with new version 2.0.4 installations.

The change in installations is a one-time occurrence that is necessary to provide better installation procedures that are fully scriptable.

[Issue 28](https://code.google.com/p/tungsten-replicator/issues/detail?id=28) - Upgrade THL to use buffered I/O

[Issue 36](https://code.google.com/p/tungsten-replicator/issues/detail?id=36) - Single command setup for replication

[Issue 42](https://code.google.com/p/tungsten-replicator/issues/detail?id=42) - Tungsten-replicator duplicates tarball expanded directory

[Issue 71](https://code.google.com/p/tungsten-replicator/issues/detail?id=71) - Enhance THL durability against failures

[Issue 82](https://code.google.com/p/tungsten-replicator/issues/detail?id=82) - Implement THL ParallelQueue Support, aka on-disk queues

[Issue 83](https://code.google.com/p/tungsten-replicator/issues/detail?id=83) - Trepctl should take the RMI port from configuration files

[Issue 93](https://code.google.com/p/tungsten-replicator/issues/detail?id=93) - Duplicate issues in static-servicename.properties

[Issue 97](https://code.google.com/p/tungsten-replicator/issues/detail?id=97) - Mysqldump options are not optimal

[Issue 98](https://code.google.com/p/tungsten-replicator/issues/detail?id=98) - Properties file uses hardcoded file names for backup and other

[Issue 99](https://code.google.com/p/tungsten-replicator/issues/detail?id=99) - Tungsten installer does not detect running MySQL native replication

[Issue 102](https://code.google.com/p/tungsten-replicator/issues/detail?id=102) - Implement shard control API

[Issue 103](https://code.google.com/p/tungsten-replicator/issues/detail?id=103) - Replicator parsing for MySQL has a number of missing cases that result in large numbers of #UNKNOWN shards

[Issue 106](https://code.google.com/p/tungsten-replicator/issues/detail?id=106) - MySQL extractor fails to extract server\_id value from first transaction in new binlog file

[Issue 109](https://code.google.com/p/tungsten-replicator/issues/detail?id=109) - Slave replicator loses track of position after a process restart

[Issue 113](https://code.google.com/p/tungsten-replicator/issues/detail?id=113) - Tungsten installer detects privileges for the wrong user

[Issue 117](https://code.google.com/p/tungsten-replicator/issues/detail?id=117) - Report null values distinctly in `./thl list` output

[Issue 120](https://code.google.com/p/tungsten-replicator/issues/detail?id=120) - Fix issues identified by RandomQueryGenerator tests

[Issue 122](https://code.google.com/p/tungsten-replicator/issues/detail?id=122) - PrimaryKeyFilter throws NullPointerException if it can't find the table

[Issue 124](https://code.google.com/p/tungsten-replicator/issues/detail?id=124) - UPDATE multiple rows with different values in a single SQL leads to inconsistency

[Issue 125](https://code.google.com/p/tungsten-replicator/issues/detail?id=125) - Tungsten-installer fails on Ubuntu 9.10

[Issue 126](https://code.google.com/p/tungsten-replicator/issues/detail?id=126) - Merge Greenplum support

[Issue 130](https://code.google.com/p/tungsten-replicator/issues/detail?id=130) - Tools/tungsten-installer does not issue warnings when the master-host is incorrect

[Issue 133](https://code.google.com/p/tungsten-replicator/issues/detail?id=133) - Make replicator use relay logs as their default

[Issue 138](https://code.google.com/p/tungsten-replicator/issues/detail?id=138) - Tungsten-installer should be able to accept properties values

[Issue 140](https://code.google.com/p/tungsten-replicator/issues/detail?id=140) - Tungsten-installer issues confusing error about server ID if MySL login credentials are wrong

[Issue 141](https://code.google.com/p/tungsten-replicator/issues/detail?id=141) - Tools/configure-service does not update services

[Issue 142](https://code.google.com/p/tungsten-replicator/issues/detail?id=142) - Tungsten-installer :: Ruby issues :: NilClass (NoMethodError)

[Issue 146](https://code.google.com/p/tungsten-replicator/issues/detail?id=146) - Tungsten-installer hangs when there is a non-breaking space in the command arguments

[Issue 151](https://code.google.com/p/tungsten-replicator/issues/detail?id=151) - Enable seamless migration from and to MySQL replication   Sponsored

[Issue 159](https://code.google.com/p/tungsten-replicator/issues/detail?id=159) - Flush operation does not return the right sequence number (returns the last but one)

[Issue 160](https://code.google.com/p/tungsten-replicator/issues/detail?id=160) - DatabaseTransformFilter fails with NullPointerException if an event does not have a default schema

[Issue 164](https://code.google.com/p/tungsten-replicator/issues/detail?id=164) - Merge MySQL->Oracle support

[Issue 167](https://code.google.com/p/tungsten-replicator/issues/detail?id=167) - Broken installation in build 192

[Issue 169](https://code.google.com/p/tungsten-replicator/issues/detail?id=169) - Trepctl offline is ignored in some states, such as during replicator synchronization with master

[Issue 170](https://code.google.com/p/tungsten-replicator/issues/detail?id=170) - Shard status list does not show latency correctly

[Issue 173](https://code.google.com/p/tungsten-replicator/issues/detail?id=173) - `./trepctl online-skip z` doesn't wait for events to be received

[Issue 176](https://code.google.com/p/tungsten-replicator/issues/detail?id=176) - Slave replicator does not reliably reconnect when master IP goes down or moves to another host

[Issue 177](https://code.google.com/p/tungsten-replicator/issues/detail?id=177) - On-disk queues fail due to apparent race condition but do not stop the replicator

[Issue 180](https://code.google.com/p/tungsten-replicator/issues/detail?id=180) - Parallel queues are not crash safe

[Issue 182](https://code.google.com/p/tungsten-replicator/issues/detail?id=182) - Tungsten-installer fails master/slave validation if OpenJDK is installed

[Issue 184](https://code.google.com/p/tungsten-replicator/issues/detail?id=184) - Race condition at start-up of THLParallelQueue causes intermittent pipeline failure

[Issue 190](https://code.google.com/p/tungsten-replicator/issues/detail?id=190) - Tungsten slave fails when restarting on clean logs after reloading a dump

[Issue 196](https://code.google.com/p/tungsten-replicator/issues/detail?id=196) - PostgreSQL streaming replication management fails on start-up

[Issue 198](https://code.google.com/p/tungsten-replicator/issues/detail?id=198) - Tungsten Replicator retention option does not purge logs

[Issue 199](https://code.google.com/p/tungsten-replicator/issues/detail?id=199) - Tungsten-installer --help-all command hangs on Ubuntu

[Issue 202](https://code.google.com/p/tungsten-replicator/issues/detail?id=202) - Configure-service concatenates properties

[Issue 203](https://code.google.com/p/tungsten-replicator/issues/detail?id=203) - Slave replicator incorrectly commits following lock wait timeout

[Issue 205](https://code.google.com/p/tungsten-replicator/issues/detail?id=205) - Heterogeneous installer: do not transfer strings as bytes

[Issue 206](https://code.google.com/p/tungsten-replicator/issues/detail?id=206) - Tungsten-installer does not check for binlog being enabled.

[Issue 207](https://code.google.com/p/tungsten-replicator/issues/detail?id=207) - Tungsten installer does not parse --native-slave-takeover correctly

[Issue 208](https://code.google.com/p/tungsten-replicator/issues/detail?id=208) - Tungsten-installer does not detect the server-id correctly

[Issue 209](https://code.google.com/p/tungsten-replicator/issues/detail?id=209) - Inserting data into a myisam table with auto\_increment primary key breaks multi master replication

## Tungsten Replicator 2.0.3 ##

This release contains a beta version of the new command line installer, a prototype applier for MySQL to MongoDB replication, and numerous bug fixes.

Logs and other data structures should be backwards-compatible with version 2.0.2.  If you run into log compatibility problems, please log them as bugs.  If in doubt, quiesce the master(s), allow all slaves to catch up, and clear THL files before upgrading.

[Issue 18](https://code.google.com/p/tungsten-replicator/issues/detail?id=18) - Tungsten replicator with binary support enabled fails on MySQL 5.5

[Issue 22](https://code.google.com/p/tungsten-replicator/issues/detail?id=22) - Missing option in configure script to set RMI port

[Issue 23](https://code.google.com/p/tungsten-replicator/issues/detail?id=23) - Missing options to set the applier in configure-service

[Issue 24](https://code.google.com/p/tungsten-replicator/issues/detail?id=24) - Replicator start in sandbox fails due to non-standard ports

[Issue 25](https://code.google.com/p/tungsten-replicator/issues/detail?id=25) - Provide JSON scripting API for replicator

[Issue 29](https://code.google.com/p/tungsten-replicator/issues/detail?id=29) - Replication failure after long delay between queries...

[Issue 30](https://code.google.com/p/tungsten-replicator/issues/detail?id=30) - WARNING from running trepctl services

[Issue 32](https://code.google.com/p/tungsten-replicator/issues/detail?id=32) - Tungsten slaves react poorly when they cannot obtain needed sequence number from master

[Issue 34](https://code.google.com/p/tungsten-replicator/issues/detail?id=34) - Use buffered I/O more effectively when extracting from MySQL binlog

[Issue 35](https://code.google.com/p/tungsten-replicator/issues/detail?id=35) - Clean up Tungten release directory structure

[Issue 37](https://code.google.com/p/tungsten-replicator/issues/detail?id=37) - Tungsten Replicator fails to extract from MySQL 5.5 binlog if clients use utf8mb4 character set

[Issue 38](https://code.google.com/p/tungsten-replicator/issues/detail?id=38) - THL connection URL is not set correctly when using off-board replication

[Issue 41](https://code.google.com/p/tungsten-replicator/issues/detail?id=41) - tungsten-installer does not check if a given directory name is a regular file

[Issue 43](https://code.google.com/p/tungsten-replicator/issues/detail?id=43) - Installation fails with tungsten-installer

[Issue 44](https://code.google.com/p/tungsten-replicator/issues/detail?id=44) - tungsten-installer does not accept --verbose, no-validation and --validate-only unless they are on top

[Issue 45](https://code.google.com/p/tungsten-replicator/issues/detail?id=45) - tungsten-installer creates directories without asking and without options to modify the defaults

[Issue 46](https://code.google.com/p/tungsten-replicator/issues/detail?id=46) - tungsten-installer missing option : --start

[Issue 47](https://code.google.com/p/tungsten-replicator/issues/detail?id=47) - Option names for the same thing should be common to all modes and have a unique name

[Issue 48](https://code.google.com/p/tungsten-replicator/issues/detail?id=48) - tungsten-installer does not check that master and slave have different sources

[Issue 49](https://code.google.com/p/tungsten-replicator/issues/detail?id=49) - Missing option in tungsten-installer in --direct-mode for master-log-directory and master-log-pattern

[Issue 50](https://code.google.com/p/tungsten-replicator/issues/detail?id=50) - tungsten-installer does not require slave-password as mandatory

[Issue 54](https://code.google.com/p/tungsten-replicator/issues/detail?id=54) - Provide a filter that extracts table columns names on the extractor side

[Issue 55](https://code.google.com/p/tungsten-replicator/issues/detail?id=55) - Create Basic MongoDB Applier

[Issue 56](https://code.google.com/p/tungsten-replicator/issues/detail?id=56) - tungsten-installer help should be shortened

[Issue 57](https://code.google.com/p/tungsten-replicator/issues/detail?id=57) - tungsten-installer should not display the full help after an error

[Issue 59](https://code.google.com/p/tungsten-replicator/issues/detail?id=59) - Setting two slaves in direct role with the same master fails

[Issue 64](https://code.google.com/p/tungsten-replicator/issues/detail?id=64) - Add a check to ensure that a master replicator does not have unapplied data in its history log (THL)

[Issue 65](https://code.google.com/p/tungsten-replicator/issues/detail?id=65) - Unable to create RMI listener due to bad IP in /etc/hosts

[Issue 68](https://code.google.com/p/tungsten-replicator/issues/detail?id=68) - After using tungsten-installer with the --direct option, thl does not work

[Issue 69](https://code.google.com/p/tungsten-replicator/issues/detail?id=69) - ./tools/configure fails when star with a custom made cfg file

[Issue 70](https://code.google.com/p/tungsten-replicator/issues/detail?id=70) - Merge improvements from Tungsten 1.3.x

[Issue 72](https://code.google.com/p/tungsten-replicator/issues/detail?id=72) - RMI port option is not applied

[Issue 75](https://code.google.com/p/tungsten-replicator/issues/detail?id=75) - tungsten-installer fails to notice that the replicator is already started

[Issue 76](https://code.google.com/p/tungsten-replicator/issues/detail?id=76) - Installer with --start-and-report fails to start correctly

[Issue 84](https://code.google.com/p/tungsten-replicator/issues/detail?id=84) - Add options for sandbox installation to master-slave

[Issue 91](https://code.google.com/p/tungsten-replicator/issues/detail?id=91) - Provide version information for replicator in easy-to-find location

## Tungsten Replicator 2.0.2 ##

This release is our first full open source release and contains a couple of critical bug fixes for parallel and multi-master replication as well as a number of smaller fixes to correct problems with installations.  Logs and other data structures are backwards-compatible with version 2.0.1.

[Issue 1](https://code.google.com/p/tungsten-replicator/issues/detail?id=1) - Clean up javadoc build warnings

[Issue 2](https://code.google.com/p/tungsten-replicator/issues/detail?id=2) - Configure script does not set up disk log

[Issue 3](https://code.google.com/p/tungsten-replicator/issues/detail?id=3) - Integrate Hudson build number to release tarball

[Issue 5](https://code.google.com/p/tungsten-replicator/issues/detail?id=5) - Replicator does not correctly assign #UNKNOWN shard when updates cross schema boundaries within a single transaction

[Issue 6](https://code.google.com/p/tungsten-replicator/issues/detail?id=6) - When using disk logs, thl list can show a NullPointerException error

[Issue 11](https://code.google.com/p/tungsten-replicator/issues/detail?id=11) - Add a timestamp column into trep\_commit\_seqno table in order to know when this table was updated

[Issue 15](https://code.google.com/p/tungsten-replicator/issues/detail?id=15) - Some records get lost in multiple master replication

[Issue 19](https://code.google.com/p/tungsten-replicator/issues/detail?id=19) - Thl unable to read disk log

[Issue 20](https://code.google.com/p/tungsten-replicator/issues/detail?id=20) - Need installation support for direct pipeline where master is on a remote host


## Tungsten Replicator 2.0.1 ##

This release is a binary build from the enterprise sources prior to full open source release.  It is free for use while we are getting code properly merged to code.google.com.

  * TUC-211 - Slave replicator freezes after going online following master restart
  * TUC-249 - Replicator consistency checks fail with NullPointerException when replicator is offline
  * TUC-266 - Parallel apply performance is unexpectedly slow when using direct pipeline
  * TUC-267 - Tungsten Replicator tasks improperly commit current block on interruption, which causes a restart failure
  * TUC-268 - race condition with DDL immediately after Tungsten starts
  * TUC-269 - Possible loop with multi-master topology due to block commit
  * TUC-270 - Simple SQL sequence causes UNKNOWN shards
  * TUC-271 - Tungsten 2.0 installation does not work properly for off-board replication