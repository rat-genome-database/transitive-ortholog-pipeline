#!/usr/bin/env bash
. /etc/profile

APPNAME=TransitiveOrthologPipeline
APPDIR=/home/rgddata/pipelines/$APPNAME

cd $APPDIR

DB_OPTS="-Dspring.config=$APPDIR/../properties/default_db.xml"
LOG4J_OPTS="-Dlog4j.configuration=file://$APPDIR/properties/log4j.properties"
export TRANSITIVE_ORTHOLOG_PIPELINE_OPTS="$DB_OPTS $LOG4J_OPTS"
bin/$APPNAME "$@" 2>&1

