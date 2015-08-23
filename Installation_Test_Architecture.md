#Architecture for testing installations

This is work related to [Issue#85](https://code.google.com/p/tungsten-replicator/issues/detail?id=#85)

The following template is a JSON structure containing all the elements necessary to test a Tungsten Replicator tarball.

```
{
    "package"           : "tungsten-replicator-2.0.3-104.tar.gz",
    "install_directory" : "/opt/continuent/tungsten_replicator/master_slave",
    /*
     * Commands to run before installing.
     */
    "hosts"             : {
        "common" : { "port": "3306", "datadir" : "/var/lib/mysql"  },
        "host1"  : { "hostname": "r1" },
        "host2"  : { "hostname": "r2" },
        "host3"  : { "hostname": "r3" },
        "host4"  : { "hostname": "r4" }
    },
    "pre_install"       : [""],
    /*
     * Commands to run to install the package.
     * you can list either "install_script" or an array of "install_commands"
     */
    "install"           : {
           "install_script"   : "master_slave.sh",
           "install_commands" : [
                        "export SOMEVAR=something",
                        "/path/to/some_command arg1 arg2",
                        "/path/to/some_other_command arg1 arg2",
                        "$install_script"
                ]
        },
    "test"              : [
            /*
             *  Available testing commands
             *      "shell"  shell command
             *      "sql"    database query
             *      "file"   various file existence test
             *      "infile" file contents test
             *      "table"  schema, table, and column existence test 
             *      "sequence" facts about a sequence in a given host
             * 
             *  Available "expect" formats
             *  "regular expression"   =>   match result  
             *  "ok"                   =>   return code == 0
             *  "!regular expression" =>   not matching result
             *  "!ok"                  =>   return code != 0
             *  "< expression"         =>   result is smaller than expression 
             *  "=< expression"        =>   result is smaller or equal to expression 
             *  "> expression"         =>   result is greater than expression 
             *  ">= expression"        =>   result is greater or equal to expression 
             *  Expect can take an array of arguments.  
             *
             * Available "when" clauses:
             * "pre_install"   test is executed after pre_install
             * "install"       test is executed after install 
             * "post_install"  test is executed after post_install
             *
             */
            { 
                "name"      : "shell",
                "when"      : "install", /* mandatory */
                "host"      : "",  /* mandatory */
                "shell"     : "",  /* mandatory */
                "expect"    : "",  /* default: 0 return code */
                "message"   : ""   /* default: shell command*/
            },
            /* 
             * File tests. Tests for existence of a give file or directory
             * Accepted tests:
             *     file : file must exist
             *     filex: file must be executable
             *     filed: file must be a directory
             */
            {
                "name"        : "file",
                "when"        : "install", /* mandatory */
                "host"        : "",  /* mandatory */
                "filename"    : "",  /* mandatory */
                "expect"      : "",  /* default: 0 return code */
                "message"     : ""   /* default: filename */
            },
            /*
             * File contents test. Tests for given contents inside a file.
             * 
             */
            { 
                "name"      : "infile",
                "when"      : "install", /* mandatory */
                "host"      : "",  /* mandatory */
                "infile"    : "",  /* mandatory */
                "expect"    : "",  /* default: 0 return code */
                "message"   : ""   /* default: filename + expect*/
            },
            /* 
             * Table tests. Tests for existence of a give table 
             * Accepted tests:
             *     schema : schema must exist
             *     table  : table must exist
             *     column : column must exist 
             */
            {
                "name"        : "table",
                "when"      : "install", /* mandatory */
                "host"        : "",  /* mandatory */
                "schema"      : "",  /* mandatory */
                "table"       : "",  /* optional */
                "column"      : "",  /* optional */
                "expect"      : "",  /* default: 0 return code */
                "message"     : ""   /* default: schema [+ table [+column]]*/
            },
            /*
             * Sequence test. Tests for a given sequence
             * 
             */
            { 
                "name"      : "sequence",
                "when"      : "install", /* mandatory */
                "host"      : "",  /* mandatory */
                "service"   : "",  /* mandatory */
                "expect"    : "",  /* no defaults */
                "message"   : ""   /* no defaults*/
            },
 
            { 
                "name"      : "sql",
                "when"      : "install", /* mandatory */
                "host"      : "",  /* mandatory */
                "sql"       : "",  /* mandatory */
                "expect"    : "",  /* default: 0 return code */
                "message"   : ""   /* default: sql command*/
            }
        ],
        /*
         * You can list either a "cleanup_script" or several "cleanup_commands"
         * 
         */
    "post_install"      : {
            "cleanup_script" : "clear_master_slave.sh"
        }
}

```