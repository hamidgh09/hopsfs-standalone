package org.hops;

import io.hops.security.UsersGroups;
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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
  static final Log LOG = LogFactory.getLog(Main.class);

  public static void main(String[] args) {
    Logger.getRootLogger().setLevel(Level.INFO);
    MiniDFSCluster cluster = null;

    final int BLKSIZE = 1 * 1024 * 1024;
    final int NUM_DN = 1;
    final int NAMENODE_PORT = 8020;

    try {
      LOG.info("Starting HopsFS standalone cluster...");

      Configuration conf = new HdfsConfiguration();
      conf.setLong(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLKSIZE);
      conf.setLong(DFSConfigKeys.DFS_NAMENODE_RETRY_CACHE_EXPIRYTIME_MILLIS_KEY, 5000);
      conf.set(DFSConfigKeys.DFS_PERMISSIONS_SUPERUSERGROUP_KEY, System.getProperty("user.name"));
      conf.setBoolean(DFSConfigKeys.DFS_PERMISSIONS_ENABLED_KEY, false);

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

      // Add test users and groups
      LOG.info("Setting up users and groups...");
      UsersGroups.addUser("gohdfs1");
      UsersGroups.addUser("gohdfs2");
      UsersGroups.addGroup("gohdfs1");
      UsersGroups.addGroup("gohdfs2");
      UsersGroups.addUserToGroups("gohdfs1", new String[]{"gohdfs1"});
      UsersGroups.addUserToGroups("gohdfs2", new String[]{"gohdfs2"});

      // Set storage policy
      dfs.setStoragePolicy(new Path("/"), "HOT");

      // Create test directory
      LOG.info("Creating test directory /_test...");
      dfs.mkdirs(new Path("/_test"), new FsPermission(0777));
      dfs.setPermission(new Path("/_test"), new FsPermission(0777));

      // Write HopsFS configuration files
      writeHopsFSConfig(cluster);

      LOG.info("======================================");
      LOG.info("HopsFS cluster is running!");
      LOG.info("NameNode address: " + cluster.getNameNode(0).getHostAndPort());
      LOG.info("HTTP address: " + cluster.getNameNode(0).getHttpAddress());
      LOG.info("Configuration written to: /tmp/hopsfs-conf/");
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

  private static void writeHopsFSConfig(MiniDFSCluster cluster) throws IOException {
    String confDir = "/tmp/hopsfs-conf";

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