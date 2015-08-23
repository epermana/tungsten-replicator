# How to submit a good database bug report #

When an open source project becomes popular, bug reports start flocking in. This is both good and bad news for the project developers. The good news is that someone is using the product, and they are finding ways of breaking it that we didn't think of. The bad news is that most of the times the reporters assume that the developers have super human powers, and that they will find what's wrong by the simple mentioning that a given feature is not working as expected. Unfortunately, it doesn't work that way. An effective bug report should have enough information that the ones in charge will be able to reproduce it and examine in lab conditions to find the problem. When dealing with databases and database tools, there are several cases, from simple to complex. Let's cover them in order.

## Installation issues ##

This is often a straightforward case of lack of functionality. When a tool does not install what it is supposed to, it is a show stopper, and a solution must be found. In this case, it's often enough to state the environment where the installation is happening (operating system, version of the tool, version of core components such as the MySQL database used) and the command used to start the installation. The error message could be an expected failure, when the installation procedure checks for requirements and fails if they are not met. For example: "Missing Ruby interpreter". The message tells (or suggests) you what to do. Filing a bug report on an expected failure is a waste of time. You should install the missing part and try again. Even if the message is about an unexpected failure (e.g. you get a stack trace from Ruby or Java), usually the first message tells you enough to be able to find a workaround. For example, if you get an exception from Ruby complaining about a missing 'curl' command, you can file a bug report to ask for the installer to check for 'curl' in the requirements, but if you install 'curl' yourself, the installation should continue.

## Simple database issues ##

Reporting a database bug means complaining that the DBMS is not behaving as advertising by the documentation, or as common usage dictates. If it is a missing or misbehaving functionality, the best way of showing the problem is by starting with an empty DBMS, followed by the creation of the objects needed to reproduce the issue (CREATE SCHEMA, TABLE, INDEX) and by a minimal amount of data that triggers the problem. Some information about what operating system and database version was used is probably necessary to reproduce the problem consistently.

## Simple database replication issues ##

By ''simple replication'' we mean a vanilla master/slave topology. In this scheme, data inserted in the master will eventually end up in the slave. Bugs in this category may fail to replicate the data totally or partially, or they may cause a break in the replication flow. Reproducing them is almost as easy as with simple database bugs. If you start with an empty system and manage to reproduce the error with a short sequence of commands, it should probably reproducible by a third party. Sometimes, settings in the master and the slave are essential to reproduce the problem. In MySQL, the format of binary logs, the default database engine and SQL modes can affect replication and produce different results with the same stream of SQL commands.

## Complex database replication issues ##

The most difficult bugs to report are the ones where the error shows up only in a given topology. While MySQL native replication offers only few options to pipe data around (single, circular, hierarchical), Tungsten replicator allows a rich set of combined pipelines that can change the outcome of a data change event, depending on the originating node and the direction it took. In these case, information o how the cluster was installed becomes essential.

## Concurrency issues ##

This is one of the most difficult bugs to report. When an error happens only because of the contemporary action of two or more threads, there is no easy way of reporting it in a way that it can be easily reproduced. Three methods are possible:

  * Describe the action of the first thread, then mark the change of thread and describe the actions of the second thread, continuing in this way until you reach the error point.
  * If you are a developer and feel comfortable with multi thread applications, write a script that reproduces the error by running several threads (Perl, Python, and Ruby offer the best environment for this kind of tests).
  * If the database offers a tool to write such multi-threading tests, consider using it.

## Heavy load issues ##

This is a more complex case of the above one. Not only you need concurrency, but a lot of it happening at the same time. Reproducing this kind or error is challenging. If you have a support agreement with the provider of the database or the tool, you may let the support engineer have a look at your running environment, to find some clues. But even in this case, the support engineers or yourself need to ultimately reproduce the case in such a way that a developer can fix the problem and test the fix. There are two methods to report this problem:

  * Simplification: if you can reduce the concurrency to the elements that are misbehaving, the methods for concurrency issues will apply also in this case.
  * Enabling a query log could lead to identifying the sequence of events that have generated the error. The log should be integrated by the DDL of the objects involved in the action (schemas, tables, triggers, views, etc).

## Large data issues ##

If your error only shows itself with large data, there is often a logistical problem, as you can't easily provide gigabytes of data, even if there privacy and security issues weren't in the way (which usually are). There are three strategies that you can use to report such bugs:

  * If only the size matters, then describing the kind of data used could be enough. E.g. ''When a table with such fields and such indexes grows beyond X GB, then the optimizer warp drive explodes.'' (Don't try this at home)
  * Create a script to generate the data that will ultimately trigger the error. This method requires both skills and an understanding of what the error is.
  * You may use a publicly available database to reproduce the error (for example, the [Sample database with test suite](https://launchpad.net/test-db).) Just mention in the bug report where to find the database, eventually how to load it if it is not simple, and then describe the steps needed to reproduce the bug after loading it.

## DOs and DONTs ##

  * DO
    * Search the bug repository and the mailing lists (or forums) before submitting yours. Someone may have had the same problem before you did.
    * **Put yourself in the receiver's position**, and try to reproduce the problem from a clean initial state.
    * If there is a workaround, mention it: it might give a good clue to the developers.
    * More information is better. Anything that can improve the identification of the bug root cause will be welcome. (But don't overdo: see below)
    * If you feel that a missing feature should be useful, report it as a feature request. (Even better: suggest a patch)

  * DON'T
    * Don't report a missing feature as a bug, unless the docs say that the feature should be there.
    * **Don't just send the error message without the events that generated it.**
    * Don't include SQL commands embedded in code.
    * Don't say "my application doesn't work anymore," assuming it's the database's fault. Remember [The First Rule of Programming: It's Always Your Fault](http://www.codinghorror.com/blog/2008/03/the-first-rule-of-programming-its-always-your-fault.html)
    * Less is better. If there is a long way and a short way of reproducing the bug, the shorter one is better. Don't send more info just for the sake of it.
    * Don't tell the developers that they are retarded. This will not increase the priority given to your bug, or your credibility.


---


Already published as [How to submit a good database bug report](http://datacharmer.blogspot.com/2011/12/how-to-submit-good-database-bug-report.html). Reproduced here with author permission