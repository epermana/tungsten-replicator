Tungsten Replicator 2.0 is 100% open source.  You can build it yourself!  Here are the steps:
  * `svn co http://tungsten-replicator.googlecode.com/svn/trunk/builder`
  * `cd builder`
  * `./build.sh`

This will create a `../sources` directory where all required components will check out and build.

The build locations and outputs are controlled by the config file.  The contents are more or less self-explanatory.

If you want to modify directories, URLs, skip the checkout, compilation or source generation, it is strongly discouraged to edit `config` directly. Rather create a `config.local` file (it is already in svn:ignore) and override flags there.
Example:
```
# cat config.local
SKIP_CHECKOUT=1
SKIP_BUILD=0
SKIP_SOURCE=0
```

**Important Note** If you are a developer and committing changes to the project you must check out using 'https' as in the following command.  Otherwise you will be unable to commit anything to the builder project.

`svn co https://tungsten-replicator.googlecode.com/svn/trunk/builder`