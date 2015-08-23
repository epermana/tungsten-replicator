
# Frequently Asked Questions #

This page answers frequently asked questions about Tungsten.

## What is the difference between Tungsten Replicator and Tungsten Enterprise? ##
Tungsten Replicator is an advanced product for database replication with features such as filters, seamless failover, parallel and multi-master replication.
[Tungsten Enterprise](http://www.continuent.com/solutions/tungsten-enterprise) is a database clustering product that builds on top of Tungsten Replicator. While the Replicator is free, the Enterprise edition is a commercial package, available to customers only.

## Is Tungsten Replicator free? ##
Yes. It is free to use and modify under the terms of the GPL V2 license.

## Where is the code? ##
The code is [here](http://code.google.com/p/tungsten-replicator/source/checkout)

## What happened to the old Continuent Community? ##

It is now here.  We do not have a community offering for the enterprise features any more.  Instead, we have open sourced 100% of the Tungsten Replicator.  You can get code downloads, log issues, and ask questions.

If you need to get to the old Tungsten forums, you can reach them [here](http://www.continuent.com/forum).

## Is Tungsten Replicator supported? ##
Yes. We support Tungsten Replicator in open source terms. You can report bugs and expect the same order of responsiveness that you get from any open source project.

## Where can I get more help? ##

Continuent offers commercial support and feature development for Tungsten Replicator 2.0.  Visit us at [our website](http://www.continuent.com) for more information.

## What are the birds on the site logo? ##

Those are the "Tungsten Swans."  The [Whooper Swan](http://en.wikipedia.org/wiki/Whooper_Swan) is the national bird of Finland, where Continuent originated.  Swans are also a symbol of beauty and grace.  Ours are replicated of course.  Also, our swans are _not_ geese.  And they are _really not_ ducks.

## Can you replicate from one SQL platform to another? Example: Replicate from MySQL to PostgreSQL? ##

Yes. Currently we can replicate from MySQL to PostgreSQL, Oracle, and MongoDB (experimental), from PostgreSQL to PostgreSQL. Please see the [official documentation](http://www.continuent.com/downloads/documentation) for more detail.


## I have a problem with the installation. Where do I go? ##

There is a [troubleshooting page](http://code.google.com/p/tungsten-replicator/wiki/Troubleshooting) in this wiki. If your problem is not covered, you may use the IRC ( [irc://freenode.net](irc://freenode.net) #tungsten) and the [mailing list](http://groups.google.com/group/tungsten-replicator-discuss).


## How do I install a master-slave replication cluster? ##

Look for `tungsten-installer` in the [Tungsten Documentation](http://www.continuent.com/downloads/documentation). There is a simple example of how to create a cluster with a single command.


## How do I add a slave to an existing cluster? ##

The procedure is almost the same used to create a master-slave cluster. You use a similar command, with `--master-slave` and `--master-host` as in the full cluster installation command. The difference is that the `--cluster-hosts` list will only contain the slave host. Tungsten will do the right thing.


## How do I handle conflicts in multiple master scenarios? ##

There is no built-in conflict resolution in Tungsten.
On the plus side, you can do multiple masters replication, which is more than what you can do with MySQL native replication.
On the down side, you need to deal with conflicts on your own. Replication is asynchronous, and thus it can't solve conflicts when it applies the data.
The best possible strategy is **self discipline**, i.e. organize your data entry in such a way that every entry database is modified only in one entry point. If you do that, Tungsten can help you enforcing this rule using the [shards API](http://scale-out-blog.blogspot.com/2011/08/system-of-record-approach-to-multi.html).

Short link to this page: [http://bit.ly/tr20\_faq](http://bit.ly/tr20_faq)