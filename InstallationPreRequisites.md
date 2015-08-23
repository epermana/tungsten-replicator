
# Tungsten Replicator Pre-Requisites #

To start Tungsten installation, there are some requirements to be met, for the operating system and for the database server.

## Operating system requirements ##

### Commands / programs that must exist ###

  * which
  * rsync (which rsync)
  * Ruby 1.8.5 or later (`ruby --version` )
  * Ruby openssl libraries ( `echo "p 'OK'" | ruby -ropenssl ` )
  * Java 1.6 or later (`java -version`)
  * GNU tar (`tar --version`)


### User requirements ###

  * The user that installs Tungsten (let's say it's _tungsten_) must be able to see the binary logs. For example, if the logs are under /var/log/mysql and the database server is run by user _mysql_, the best strategy is to make these logs readable by the group, and to add the _mysql_ group to _tungsten_. Check it with `ls -l /var/log/mysql/`
  * The user must be able to connect to all the servers with `ssh`, without password. Typically, you do this by creating a SSH key without a passphrase, or [using ssh-agent](http://www.unixwiz.net/techtips/ssh-agent-forwarding.html).
  * The user should be allowed to run `sudo` commands without password. This requirement can be lifted if you don't plan to do backup and restore through the replicator
    * The sudo access should be unrestricted by TTY requirements. If the option "RequireTTY" is set in /etc/sudoers, it must be removed.


## Network Requirements ##

  * All the hosts in the replication cluster must be able to see each other and to connect to each other with unique hostnames.
  * `hostname --ip-address` must return a single IP.
  * Replication ports must be open. By default:
    * 3306 (MySQL database)
    * 2112 (Tungsten THL)
    * 10000 (Tungsten RMI)
    * 10001 (JMX management)
If you change the default ports, make sure they are open.

## MySQL Requirements ##

  * As of June 2012, Tungsten supports database servers from 5.0 to 5.5. Future version compatibility will be tested and announced in due time.
  * The database server must support InnoDB
  * the default storage engine should be set to Innodb
  * there must be a user that has all privileges, including GRANT option, and can connect to every database server from every host. (Check that you can connect using `mysql -u USER -pPASSWORD -P PORT -h HOST`)
  * Binary logs must be enables, and a fixed pattern used for log names (e.g. `log-bin=mysql-bin`
  * Each server must have a unique server-id
  * The database server should be configured for good performance, according to best practices. The following properties are highly recommended:
```
    [mysqld]
    innodb-file-per-table=1
    server-id=###
    log-bin=mysql-bin
    innodb-flush-method=O_DIRECT
    max_allowed_packet=52M
    innodb-thread-concurrency=0
    default-storage-engine=innodb
```