# Add the path to GemFire lib directory
export GEODE_LIB=/Users/pvermeulen/Projects/gemfire-redis-environment/vmware-gemfire-9.15.2/lib/*
java -cp target/gemfire-metrics-1.0.0-SNAPSHOT.jar:jars/*:$GEODE_LIB -Dcapture-jvm-gemfire-stats=false -Dmetrics-port=9114 com.vmware.data.gemfire.metrics.ApplicationServerMetricsClient
