#!/usr/bin/env bash

SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
APPNAME=TransitiveOrthologPipeline
APPDIR=/home/rgddata/pipelines/$APPNAME
EMAILLIST=cdursun@mcw.edu,mtutaj@mcw.edu

if [ "$SERVER" == "REED" ]; then
  EMAILLIST=rgd.developers@mcw.edu,jrsmith@mcw.edu
fi

species_list=( 2 3 4 5 6 7 8 )

echo "" > $APPDIR/run.log
for species in "${species_list[@]}"; do
    $APPDIR/run.sh "$species" 2>&1 >> $APPDIR/run.log
done

mailx -s "[$SERVER] transitive orthos for all species" $EMAILLIST < $APPDIR/logs/summary.log
