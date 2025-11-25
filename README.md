# HopsFS Standalone

A standalone HopsFS MiniDFSCluster for testing and development.

## Prerequisites

- Java 8 or higher
- Maven 3.x
- Access to HopsFS Maven repositories (configured in your Maven settings.xml)

## Project Structure

```
src/main/java/org/hops/Main.java              - Standalone cluster runner
```

## Building

Build a fat JAR with all dependencies:

```bash
mvn clean package -DskipTests
```

This creates a standalone JAR at `target/hopsfs-standalone-1.0-SNAPSHOT.jar` that can run on any machine with Java.

## Running

```bash
java -jar target/hopsfs-standalone-1.0-SNAPSHOT.jar
```

Or with options:
```bash
java -jar target/hopsfs-standalone-1.0-SNAPSHOT.jar --num-datanodes=3 --namenode-port=8020
```

## Command-Line Options

```
  --num-datanodes=N       Number of DataNodes (default: 1)
  --num-namenodes=N       Number of NameNodes (default: 1)
  --namenode-port=N       NameNode port (default: 8020)
  --conf-dir=PATH         Configuration output directory (default: /tmp/hopsfs-conf)
  --ndb-config=PATH       NDB configuration file (default: ndb-config.properties)
  --dfs-base-dir=PATH     DFS data directory (default: /tmp/hopsfs-data)
  -h, --help              Show this help message
```

## NDB Configuration

Use a custom NDB configuration file:

```bash
java -jar target/hopsfs-standalone-1.0-SNAPSHOT.jar --ndb-config=/etc/hopsfs/ndb-config.properties
```

Or override individual settings with system properties:

```bash
java -Dcom.mysql.clusterj.connectstring=ndb-host:1186 \
     -Dcom.mysql.clusterj.database=hops \
     -Dio.hops.metadata.ndb.mysqlserver.host=mysql-host \
     -Dio.hops.metadata.ndb.mysqlserver.port=3306 \
     -Djava.library.path=/path/to/ndb/lib \
     -jar target/hopsfs-standalone-1.0-SNAPSHOT.jar
```

See `src/main/resources/ndb-config.properties` for all available options.


## Output Files

After starting, configuration files are written to `--conf-dir` (default: `/tmp/hopsfs-conf`):
- `hdfs-site.xml` - HDFS configuration for client applications
- `hopsfs-uri.txt` - NameNode hostname:port

## Examples

### Quick Start
```bash
java -jar target/hopsfs-standalone-1.0-SNAPSHOT.jar
```
Starts a cluster with 1 NameNode (port 8020), 1 DataNode, configs in `/tmp/hopsfs-conf/`, data in `/tmp/hopsfs-data/`.

### Custom Cluster
```bash
java -jar target/hopsfs-standalone-1.0-SNAPSHOT.jar \
     --num-namenodes=2 \
     --num-datanodes=3 \
     --namenode-port=9000 \
     --conf-dir=/var/lib/hopsfs/config \
     --dfs-base-dir=/var/lib/hopsfs/data
```

### With Custom NDB Config
```bash
java -jar target/hopsfs-standalone-1.0-SNAPSHOT.jar \
     --ndb-config=/etc/hopsfs/ndb-config.properties \
     --num-datanodes=2
```

## Stopping

Press `Ctrl+C` to gracefully shutdown the cluster.
