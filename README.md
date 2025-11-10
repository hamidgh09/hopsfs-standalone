# HopsFS Standalone

A standalone HopsFS MiniDFSCluster for testing and development.

## Prerequisites

- Java 8 or higher
- Maven 3.x
- Access to HopsFS Maven repositories (configured in your Maven settings.xml)

## Project Structure

```
src/main/java/org/hops/Main.java              - Standalone cluster runner
src/test/java/org/hops/TestHopsFSMountSimple.java - JUnit test examples
```

## Building

```bash
mvn clean compile
```

## Running

You can run the standalone HopsFS cluster using Maven with increased JVM settings to avoid CodeCache issues:

```bash
export MAVEN_OPTS="-Xmx2g -Xms1g -XX:ReservedCodeCacheSize=512m"
mvn exec:java
```

Or compile and run in one command:

```bash
export MAVEN_OPTS="-Xmx2g -Xms1g -XX:ReservedCodeCacheSize=512m"
mvn clean compile exec:java
```

Alternatively, you can set these as a one-liner:

```bash
MAVEN_OPTS="-Xmx2g -Xms1g -XX:ReservedCodeCacheSize=512m" mvn clean compile exec:java
```

## What it does

The Main class will:
1. Start a MiniDFSCluster with 1 DataNode on port 8020
2. Create test users (gohdfs1, gohdfs2) and groups
3. Set the storage policy to HOT
4. Create a test directory `/_test` with 777 permissions
5. Write configuration files to `/tmp/hopsfs-conf/`:
   - `hdfs-site.xml` - Full HDFS configuration
   - `hopsfs-uri.txt` - NameNode address

The cluster will run indefinitely until you press Ctrl+C.

## Configuration Files

After starting, you can find the configuration files at:
- `/tmp/hopsfs-conf/hdfs-site.xml` - Full HDFS configuration for client applications
- `/tmp/hopsfs-conf/hopsfs-uri.txt` - NameNode hostname:port

## Connecting to the Cluster

Once running, you can connect to the cluster at:
- **NameNode RPC**: `localhost:8020` (or the address shown in the startup logs)
- The exact address will be printed to the console when the cluster starts

Example connection from another Java application:
```java
Configuration conf = new Configuration();
conf.addResource(new Path("/tmp/hopsfs-conf/hdfs-site.xml"));
FileSystem fs = FileSystem.get(conf);
```

## Running Tests

The test class `TestHopsFSMountSimple` contains additional examples of working with the cluster:

```bash
mvn test
```

## Stopping

Press `Ctrl+C` to gracefully shutdown the cluster. A shutdown hook will ensure proper cleanup.
