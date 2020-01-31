#!/usr/bin/env bash
#
# as an cmdline parameter, pass the species type key (f.e. 3), or common species name (f.e. rat)
#  to run the pipeline for given species

. /etc/profile

APPNAME=TransitiveOrthologPipeline
APPDIR=/home/rgddata/pipelines/$APPNAME

cd $APPDIR

java -Dspring.config=$APPDIR/../properties/default_db.xml \
    -Dlog4j.configuration=file://$APPDIR/properties/log4j.properties \
    -jar lib/$APPNAME.jar "$@" 2>&1

