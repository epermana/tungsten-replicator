# Background #

Tungsten Replicator uses filters at different stages of data transferring.
The filters can be written either in Java or Javascript.

While this feature makes the replicator very powerful and versatile, the method for enabling a filter is quite awkward and error prone.


# Filter infrastructure changes #

To improve the usability of Tungsten filters, we need to extend the current infrastructure in two directions:

  * Create an API that will make easy for users to create and modify filters;
  * Simplify the registration of the filters with the replicator, to allow easy insertion and removal of filters.


# Filter management interface #

From the user's standpoint, filter management needs to be a linear operation. Users should be able to insert filters without editing the properties file, and without knowing arcane properties names.


## Options for tungsten-installer and configure-service ##

These options may be necessary when a filter need to be inserted or modified right when Tungsten starts.
The same result can probably be achieved with trepctl options.

```
    --add-filter=filter_name:stage:filename
    --delete-filter=filter_name:stage
    --update-filter=filter_name:stage:filename
```


## Options for trepctl ##

Probably the most useful interface for filter management will be through the standard **trepctl**, the Tungsten Replicator ConTroLler.

```
   trepctl [-service svc_name] filter -add filter_name -file filename -stage stage_name
   trepctl [-service svc_name] filter -delete filter_name [-stage stage_name]
   trepctl [-service svc_name] filter -update filter_name -file filename [-stage stage_name]
   trepctl [-service svc_name] filter -enable filter_name [-stage stage_name]
   trepctl [-service svc_name] filter -disable filter_name [-stage stage_name]
```

A filter is identified by service, filter name, stage. When there is only one service installed, the service name can be omitted.
When inserting a filter, the stage is mandatory. In other cases, if there is no other filter with the same name, the filter name will be sufficient.
The replicator will complain when users try to modify a filter without providing enough elements to identify it uniquely.


### Filter dependencies ###

A filter can indicate dependencies, i.e.

  * which filters are needed
  * which filters are incompatible with this one
  * which properties are needed for this filter to work (e.g. RBR)


## On-the-fly filters ##

Probably dreaming, but here's a wish list:

```

## rename schemas

   trepctl [-service svc_name] filter -add filter_name \
      -stage stage_name 
      -from schema=schema_name \
      -to schema=new_schema_name

## renames tables

   trepctl [-service svc_name] filter -add filter_name \
      -stage stage_name 
      -from table=schema_name.table_name \
      -to table=new_schema_name.new_table_name

## renames columns 
   trepctl [-service svc_name] filter -add filter_name \
      -stage stage_name 
      -from column=schema_name.table_name.column_name \
      -to column=new_schema_name.new_table_name.column_name

## modifies content 
   trepctl [-service svc_name] filter -add filter_name \
      -stage stage_name 
      -from column=schema_name.table_name.column_name \
      -content regex=/Parmesan/Camembert/



# or, perhaps better:

   trepctl [-service svc_name] filter -add filter_name \
      -stage stage_name \
      -rename schema=old_schema_name/new_schema_name \
      -rename schema=another_schema_name/another_new_schema_name
      -rename table=old_table_name/new_table_name



```