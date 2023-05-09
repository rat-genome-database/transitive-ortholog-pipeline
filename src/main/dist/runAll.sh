#!/usr/bin/env bash

SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
APPNAME=transitive-ortholog-pipeline
APPDIR=/home/rgddata/pipelines/$APPNAME
EMAILLIST=mtutaj@mcw.edu

if [ "$SERVER" == "REED" ]; then
  EMAILLIST=rgd.devops@mcw.edu,jrsmith@mcw.edu
fi

# run for all species in RGD, except human (transitive orthologs are made between human and given species)
$APPDIR/run.sh "0" 2>&1 > $APPDIR/run.log

mailx -s "[$SERVER] transitive orthos for all species" $EMAILLIST < $APPDIR/logs/summary.log
