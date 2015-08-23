
Up to [the Tungsten Replicator Cookbook](TungstenReplicatorCookbook.md)

# Multi-Master Installation #

## Install bi-directional replication ##

Rather than doing it manually, you can use [the cookbook](http://code.google.com/p/tungsten-replicator/wiki/TRCBasicInstallation#Using_the_cookbook:_choose_topology_and_customize_your_installat) for each of these recipes.



![https://lh5.googleusercontent.com/_gVfZHGgf5LA/TXO2MfOUagI/AAAAAAAABEc/jvt9lZC8uvY/Tungsten_bi_directional_replication.png](https://lh5.googleusercontent.com/_gVfZHGgf5LA/TXO2MfOUagI/AAAAAAAABEc/jvt9lZC8uvY/Tungsten_bi_directional_replication.png)

Using  [the cookbook](http://code.google.com/p/tungsten-replicator/wiki/TRCBasicInstallation#Using_the_cookbook:_choose_topology_and_customize_your_installat), you can edit the file NODES\_ALL\_MASTERS.sh to use only two nodes, and then run

`./cookbook/install_all_masters.sh`

## Install bi-directional replication with an additional slave ##

The recipe is similar to [TRCMultiMasterInstallation#Install\_bi-directional\_replication](TRCMultiMasterInstallation#Install_bi-directional_replication.md).
Assuming that we have a candidate slave m3.mynetwork.com, which we want to be slave of m2, what we will do is simply add this information to the second installation command:

```
SLAVE1=m3.mynetwork.com

  ./tools/tungsten-installer \
    --master-slave \
    --master-host=$MASTER2 \
    --datasource-user=tungsten \
    --datasource-password=secret \
    --service-name=delta \
    --home-directory=$TUNGSTEN_HOME \
    --cluster-hosts=$MASTER2,SLAVE1 \
    --start-and-report
```

The only difference is SLAVE1 in the --cluster-hosts list, and Tungsten will take care of the rest.

## Install a three masters replication ##

Using  [the cookbook](http://code.google.com/p/tungsten-replicator/wiki/TRCBasicInstallation#Using_the_cookbook:_choose_topology_and_customize_your_installat), you can edit the file NODES\_ALL\_MASTERS.sh to use only three nodes, and then run

`./cookbook/install_all_masters.sh`

## Install a four masters replication ##

Using  [the cookbook](http://code.google.com/p/tungsten-replicator/wiki/TRCBasicInstallation#Using_the_cookbook:_choose_topology_and_customize_your_installat), you can edit the file NODES\_ALL\_MASTERS.sh, and then run

`./cookbook/install_all_masters.sh`

![https://lh4.googleusercontent.com/-gAnEaWOQK40/Tm2NpEWCT0I/AAAAAAAABMc/M3zIn7PdIQg/s720/Tungsten_four_masters_replication.png](https://lh4.googleusercontent.com/-gAnEaWOQK40/Tm2NpEWCT0I/AAAAAAAABMc/M3zIn7PdIQg/s720/Tungsten_four_masters_replication.png)

Up to [the Tungsten Replicator Cookbook](TungstenReplicatorCookbook.md)


## Install a star topology ##

In a star topology, all the nodes are masters, passing information through a central point, named "hub".
To define your cluster, using  [the cookbook](http://code.google.com/p/tungsten-replicator/wiki/TRCBasicInstallation#Using_the_cookbook:_choose_topology_and_customize_your_installat), you can edit the files COMMON\_NODES.sh and eventually NODES\_STAR.sh, and then run

`./cookbook/install_star.sh`


## install a fan-in topology ##

In a fan\_in topology, there is one slave, receiving data from several masters.
To define your cluster, using  [the cookbook](http://code.google.com/p/tungsten-replicator/wiki/TRCBasicInstallation#Using_the_cookbook:_choose_topology_and_customize_your_installat), you can edit the files COMMON\_NODES.sh and eventually NODES\_FAN\_IN.sh, and then run

`./cookbook/install_fan_in.sh`