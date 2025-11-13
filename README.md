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

Build a fat JAR with all dependencies:

```bash
mvn clean package -DskipTests
```

This creates a standalone JAR at `target/hopsfs-standalone-1.0-SNAPSHOT.jar` that can run on any machine with Java.

## Running

### Option 1: Direct JAR execution

```bash
java -jar target/hopsfs-standalone-1.0-SNAPSHOT.jar
```

Or with parameters:
```bash
java -jar target/hopsfs-standalone-1.0-SNAPSHOT.jar --num-datanodes=3 --namenode-port=9000
```

### Option 2: Using Maven (for development)

```bash
export MAVEN_OPTS="-Xmx2g -Xms1g -XX:ReservedCodeCacheSize=512m"
mvn exec:java
```

## Command-Line Options

```
  -n, --num-datanodes=N    Number of DataNodes (default: 1)
  -p, --namenode-port=N    NameNode port (default: 8020)
  -b, --block-size=N       Block size in bytes (default: 134217728)
  -c, --conf-dir=PATH      Configuration output directory (default: /tmp/hopsfs-conf)
  --dfs-base-dir=PATH      DFS data directory (default: /tmp/hopsfs-data)
  -h, --help               Show help message
```

## Configuring NDB Connection

Override NDB cluster settings using system properties:

```bash
java -Dcom.mysql.clusterj.connectstring=ndb-host:1186 \
     -Dcom.mysql.clusterj.database=hops \
     -Dio.hops.metadata.ndb.mysqlserver.host=mysql-host \
     -Dio.hops.metadata.ndb.mysqlserver.port=3306 \
     -Djava.library.path=/path/to/ndb/lib \
     -Dlog4j2.configurationFile=log4j2.xml \
     -jar target/hopsfs-standalone-1.0-SNAPSHOT.jar --num-datanodes=3
```

See `src/main/resources/ndb-config.properties` for all available NDB configuration options.

## Using Custom Configuration Files

To use your own `ndb-config.properties` file, you must use the `-cp` (classpath) option instead of `-jar`. This allows Java to find your custom configuration file before the bundled one.

### Using `-cp` to load custom configuration

Place your configuration files in a directory (e.g., `/etc/hopsfs/config/`) and add it to the classpath:

```bash
# Linux/Mac
java -cp "target/hopsfs-standalone-1.0-SNAPSHOT.jar:/etc/hopsfs/config" \
     org.hops.Main \
     --num-datanodes=3
```

```bash
# Windows
java -cp "target/hopsfs-standalone-1.0-SNAPSHOT.jar;C:\hopsfs\config" ^
     org.hops.Main ^
     --num-datanodes=3
```

**Important**: When using `-cp`, you must:
- Specify the main class `org.hops.Main` instead of using `-jar`
- Use `:` as the classpath separator on Linux/Mac, `;` on Windows
- List your custom config directory **after** the JAR file in the classpath
- Files in your directory will override the bundled resources (like `ndb-config.properties`)

**Example directory structure:**
```
/etc/hopsfs/config/
├── ndb-config.properties      # Your custom NDB configuration
└── log4j2.properties          # Optional: custom logging configuration
```

Then run:
```bash
java -cp "target/hopsfs-standalone-1.0-SNAPSHOT.jar:/etc/hopsfs/config" org.hops.Main
```

The application will use `/etc/hopsfs/config/ndb-config.properties` instead of the bundled one.


## Configuration Files

After starting, you can find the configuration files at the directory specified by `--conf-dir` (default: `/tmp/hopsfs-conf`):
- `hdfs-site.xml` - Full HDFS configuration for client applications
- `hopsfs-uri.txt` - NameNode hostname:port

Example with custom configuration directory:
```bash
java -jar target/hopsfs-standalone-1.0-SNAPSHOT.jar --conf-dir=/home/user/hopsfs-config
```

Configuration files will be written to `/home/user/hopsfs-config/`.

## Complete Usage Examples

### Example 1: Quick Start with Defaults
```bash
java -jar target/hopsfs-standalone-1.0-SNAPSHOT.jar
```
This starts a cluster with:
- 1 DataNode on port 8020
- Block size: 128MB
- Configs written to `/tmp/hopsfs-conf/`
- Data stored in `/tmp/hopsfs-data/`
- Using bundled `ndb-config.properties`

### Example 2: Custom Cluster Configuration
```bash
java -jar target/hopsfs-standalone-1.0-SNAPSHOT.jar \
     --num-datanodes=3 \
     --namenode-port=9000 \
     --conf-dir=/var/lib/hopsfs/config \
     --dfs-base-dir=/var/lib/hopsfs/data
```

### Example 3: Using Custom NDB Config from Classpath
```bash
# Place your ndb-config.properties in /etc/hopsfs/
java -cp "target/hopsfs-standalone-1.0-SNAPSHOT.jar:/etc/hopsfs" \
     org.hops.Main \
     --num-datanodes=2 \
     --conf-dir=/home/user/hopsfs-configs
```

### Example 4: Development with System Property Overrides
```bash
java -Dcom.mysql.clusterj.connectstring=localhost:13000 \
     -Dcom.mysql.clusterj.database=hops_dev \
     -jar target/hopsfs-standalone-1.0-SNAPSHOT.jar \
     --num-datanodes=1 \
     --namenode-port=8020
```

Note: System properties (like `-Dcom.mysql.clusterj.connectstring`) will override values in `ndb-config.properties`.

## Stopping

Press `Ctrl+C` to gracefully shutdown the cluster. A shutdown hook will ensure proper cleanup.
