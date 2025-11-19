package org.hops;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.HdfsConfiguration;
import org.apache.hadoop.hdfs.MiniDFSCluster;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import static org.apache.hadoop.fs.CommonConfigurationKeys.IPC_SERVER_RPC_READ_THREADS_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_DATANODE_HANDLER_COUNT_KEY;
import static org.apache.hadoop.hdfs.DFSConfigKeys.DFS_NAMENODE_HANDLER_COUNT_KEY;

public class Main {

  static final Log LOG = LogFactory.getLog(Main.class);

  private static class ClusterConfig {
    int numDataNodes = 1;
    int blockSize = 128 * 1024 * 1024;  // 128MB
    int nameNodePort = 8020;
    String confDir = "/tmp/hopsfs-conf";
    String ndbConfigFile = "ndb-config.properties";  // Default: bundled resource
    String dfsBaseDir = "/tmp/hopsfs-data";  // Default: temporary directory
  }

  private static ClusterConfig parseCommandLineArgs(String[] args) {
    ClusterConfig config = new ClusterConfig();

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("--num-datanodes") || args[i].equals("-n")) {
        if (i + 1 < args.length) {
          config.numDataNodes = Integer.parseInt(args[++i]);
        }
      } else if (args[i].startsWith("--num-datanodes=")) {
        config.numDataNodes = Integer.parseInt(args[i].substring("--num-datanodes=".length()));
      } else if (args[i].equals("--namenode-port") || args[i].equals("-p")) {
        if (i + 1 < args.length) {
          config.nameNodePort = Integer.parseInt(args[++i]);
        }
      } else if (args[i].startsWith("--namenode-port=")) {
        config.nameNodePort = Integer.parseInt(args[i].substring("--namenode-port=".length()));
      } else if (args[i].equals("--block-size") || args[i].equals("-b")) {
        if (i + 1 < args.length) {
          config.blockSize = Integer.parseInt(args[++i]);
        }
      } else if (args[i].startsWith("--block-size=")) {
        config.blockSize = Integer.parseInt(args[i].substring("--block-size=".length()));
      } else if (args[i].equals("--conf-dir") || args[i].equals("-c")) {
        if (i + 1 < args.length) {
          config.confDir = args[++i];
        }
      } else if (args[i].startsWith("--conf-dir=")) {
        config.confDir = args[i].substring("--conf-dir=".length());
      } else if (args[i].equals("--ndb-config")) {
        if (i + 1 < args.length) {
          config.ndbConfigFile = args[++i];
        }
      } else if (args[i].startsWith("--ndb-config=")) {
        config.ndbConfigFile = args[i].substring("--ndb-config=".length());
      } else if (args[i].equals("--dfs-base-dir")) {
        if (i + 1 < args.length) {
          config.dfsBaseDir = args[++i];
        }
      } else if (args[i].startsWith("--dfs-base-dir=")) {
        config.dfsBaseDir = args[i].substring("--dfs-base-dir=".length());
      } else if (args[i].equals("--help") || args[i].equals("-h")) {
        printUsage();
        System.exit(0);
      }
    }

    return config;
  }

  private static void printUsage() {
    System.out.println("HopsFS Standalone Cluster");
    System.out.println("Usage: java -jar hopsfs-standalone.jar [options]");
    System.out.println();
    System.out.println("Options:");
    System.out.println("  -n, --num-datanodes=N    Number of DataNodes (default: 1)");
    System.out.println("  -p, --namenode-port=N    NameNode port (default: 8020)");
    System.out.println("  -b, --block-size=N       Block size in bytes (default: 134217728)");
    System.out.println("  -c, --conf-dir=PATH      Configuration output directory (default: /tmp/hopsfs-conf)");
    System.out.println("  --ndb-config=PATH        NDB configuration file (default: ndb-config.properties)");
    System.out.println("  --dfs-base-dir=PATH      DFS data directory (default: /tmp/hopsfs-data)");
    System.out.println("  -h, --help               Show this help message");
    System.out.println();
    System.out.println("System Properties:");
    System.out.println("  -Djava.library.path=/path/to/ndb/lib");
    System.out.println();
    System.out.println("Example:");
    System.out.println("  java -jar hopsfs-standalone.jar --num-datanodes=3 --namenode-port=9000");
  }

  public static void main(String[] args) {
    MiniDFSCluster cluster = null;

    ClusterConfig config = parseCommandLineArgs(args);

    final int BLKSIZE = config.blockSize;
    final int NUM_DN = config.numDataNodes;
    final int NAMENODE_PORT = config.nameNodePort;

    try {
      LOG.info("Starting HopsFS standalone cluster...");
      LOG.info("Configuration parameters:");
      LOG.info("  Number of DataNodes: " + NUM_DN);
      LOG.info("  NameNode port: " + NAMENODE_PORT);
      LOG.info("  Block size: " + BLKSIZE + " bytes");
      LOG.info("  Configuration output directory: " + config.confDir);
      LOG.info("  NDB config file: " + config.ndbConfigFile);
      LOG.info("  DFS base directory: " + config.dfsBaseDir);

      Configuration conf = new HdfsConfiguration();
      conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLKSIZE);
      conf.setLong(DFSConfigKeys.DFS_NAMENODE_RETRY_CACHE_EXPIRYTIME_MILLIS_KEY, 5000);
      conf.set(DFSConfigKeys.DFS_PERMISSIONS_SUPERUSERGROUP_KEY, System.getProperty("user.name"));
      conf.setBoolean(DFSConfigKeys.DFS_PERMISSIONS_ENABLED_KEY, false);
      conf.setStrings(DFSConfigKeys.DFS_STORAGE_DRIVER_CONFIG_FILE, config.ndbConfigFile);
      conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, config.dfsBaseDir);
      conf.set(DFS_DATANODE_HANDLER_COUNT_KEY, Integer.toString(50));
      conf.set(DFS_NAMENODE_HANDLER_COUNT_KEY, Integer.toString(50));
      conf.set(IPC_SERVER_RPC_READ_THREADS_KEY, Integer.toString(5));

      LOG.info("Building MiniDFSCluster...");
      cluster = new MiniDFSCluster.Builder(conf)
          .nameNodePort(NAMENODE_PORT)
          .numDataNodes(NUM_DN)
          .format(true)
          .build();

      FileSystem fs = cluster.getFileSystem(0);
      DistributedFileSystem dfs = (DistributedFileSystem) FileSystem
          .newInstance(fs.getUri(), fs.getConf());

      LOG.info("Cluster started successfully!");

      // Set storage policy
      dfs.setStoragePolicy(new Path("/"), "HOT");

      // Create test directory
      LOG.info("Creating test directory /_test...");
      dfs.mkdirs(new Path("/_test"), new FsPermission(0777));
      dfs.setPermission(new Path("/_test"), new FsPermission(0777));

      // Write HopsFS configuration files
      writeHopsFSConfig(cluster, config.confDir);

      LOG.info("======================================");
      LOG.info("HopsFS cluster is running!");
      LOG.info("NameNode address: " + cluster.getNameNode(0).getHostAndPort());
      LOG.info("HTTP address: " + cluster.getNameNode(0).getHttpAddress());
      LOG.info("Configuration written to: " + config.confDir);
      LOG.info("======================================");
      LOG.info("Press Ctrl+C to shutdown...");

      // Add shutdown hook
      final MiniDFSCluster finalCluster = cluster;
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        LOG.info("Shutting down cluster...");
        if (finalCluster != null) {
          finalCluster.shutdown();
        }
        LOG.info("Cluster shutdown complete.");
      }));

      // Keep the cluster running
      Thread.sleep(Long.MAX_VALUE);

    } catch (InterruptedException e) {
      LOG.info("Cluster interrupted, shutting down...");
    } catch (Exception e) {
      LOG.error("Error running HopsFS standalone cluster", e);
      e.printStackTrace();
    } finally {
      if (cluster != null) {
        cluster.shutdown();
      }
    }
  }

  private static void writeHopsFSConfig(MiniDFSCluster cluster, String confDir) throws IOException {
    File file = new File(confDir);
    file.mkdirs();

    cluster.getConfiguration(0).set("fs.defaultFS",
        "hdfs://" + cluster.getNameNode(0).getHostAndPort());

    FileOutputStream os = new FileOutputStream(confDir + "/hdfs-site.xml");
    cluster.getConfiguration(0).writeXml(os);
    os.close();

    FileWriter writer = new FileWriter(confDir + "/hopsfs-uri.txt");
    writer.write(cluster.getNameNode(0).getHostAndPort());
    writer.close();

    LOG.info("Configuration files written to " + confDir);
  }
}