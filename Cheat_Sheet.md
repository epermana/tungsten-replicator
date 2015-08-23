# Tungsten Replication for MySQL users #

MySQL replication is very well known and understood. Comparatively, Tungsten replication is quite young and unknown.  To close the gap, we offer a quick glossary of Tungsten commands with MySQL equivalents, where these exist.


---

This is work in progress. Please check this page from time to time as we will add more content.

---


## Starting and Stopping Replication ##

Tungsten replication uses services.  There may be more than one service per replicator, in which case you must use the `-service name` argument to specify which service to which commands apply.

| **Action** | **MySQL** | **Tungsten** |
|:-----------|:----------|:-------------|
| Start replication| `START SLAVE ` | `[sudo service] replicator start` |
| Start individual replication service | `START SLAVE SQL_THREAD`| `trepctl -service name online` |
| Stop individual replication service | `STOP SLAVE SQL_THREAD` | `trepctl -service name offline` |
| Stop replication completely | `STOP SLAVE` | `[sudo service] replicator stop` |

## Status ##
| **Action** | **MySQL** | **Tungsten** |
|:-----------|:----------|:-------------|
| Check all replication services  | (None)    | `trepctl services` |
| Check status of one service  | (None)    | `trepctl -service name status` |
| Check master status | `SHOW MASTER STATUS` | `trepctl -host master_host status`|
| Check slave status | `SHOW SLAVE STATUS` | `trepctl -host slave_host status` |
| Check detailed task status | (None)    | `trepctl status -name tasks` |
| Check shard definitions | (None)    | `trepctl status -name shards` |
| Check detailed store status | (None)    | `trepctl status -name stores` |
| Check slave lag | Look at `Seconds behind master` in `SHOW SLAVE STATUS` | Look at `appliedLatency` in `trepctl status` |

## Handling Errors ##

| **Action** | **MySQL** | **Tungsten** |
|:-----------|:----------|:-------------|
|  Find out reason for error | Look at `Last_Error` in `SHOW SLAVE STATUS` | Look at `pendingErrorSeqno` and `pendingExceptionMessage` from `trepctl status`|
| Get even more information | (None)    | Look in replicator log in `trepsvc.log` |
| Look up a failed transaction | Use `mysqlbinlog` | `thl -service name list -seqno error_seqno` |
| Skip over failing transaction(s) on slave | `SET GLOBAL sql_slave_skip_counter = N` | `trepctl online -skip-seqno X,Y-Z` |

## Administration ##

The `thl` command only requires a service name if you have more than one per replicator.

| Inspect replicator log | `mysqlbinlog binlog_name` | `thl -service name list [-seqno seqno]` |
|:-----------------------|:--------------------------|:----------------------------------------|
| Dump a single log file | `mysqlbinlog binlog_name` | `thl list -file thl_log_file`           |
| Show which transactions are in which log file | (None)                    | `thl index`                             |
| Purge logs             | `PURGE BINARY LOGS`       | `thl purge -high seqno_to_purge_to`     |
| Remove corrupt transaction from tail of log | (None)                    | `thl purge -low first_seqno_to_truncate` |
| Check log integrity    | (None)                    | `thl info`                              |

## Changing Replication Settings ##

Replication must go offline or restart to change configuration settings.

| **Action** | **MySQL** | **Tungsten** |
|:-----------|:----------|:-------------|
| Set master | `CHANGE MASTER TO master_host='host1'` | `trepctl -service name setrole -role slave -uri thl://host1/ ` |
| Change global parameters | Edit `my.cnf` | Edit `static-service.properties` |
| Reread configuration settings | `service mysql restart` | `trepctl configure` or `replicator restart` |
| Don't bring replication online by default | `skip-slave-start=1` | `replicator.auto_enable=false` |
| Set log retention to 7 days | `expire_logs_days=7`  | `replicator.store.thl.log_file_retention=7d` |
| Log slave updates | `log-slave-updates=1`  | `replicator.log.slave.updates=true`|

## Resetting Replication ##

These are destructive commands.  Use them at your peril.  Reset commands should with the replicator stopped or all services offline.

| **Action** | **MySQL** | **Tungsten** |
|:-----------|:----------|:-------------|
| Reset replication | `RESET MASTER` | remove thl files; drop tungsten\_service database |
| Provision slave | Restore backup then `START SLAVE` | remove thl files; restore backup; `replicator start` |

Short link to this page: [http://bit.ly/tr20\_cheat](http://bit.ly/tr20_cheat)