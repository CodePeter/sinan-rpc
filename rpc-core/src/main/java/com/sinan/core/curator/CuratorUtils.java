package com.sinan.core.curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

public class CuratorUtils {

    private static final String PATH = "/mytest-curator/test6";
    private static final String ADDRESS = "172.17.5.162:2181";
    private static final String BASE = "mytestspace";
    private static CuratorFramework client;
    static {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        client = CuratorFrameworkFactory.builder()
                .connectString(ADDRESS)
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(5000)
                .retryPolicy(retryPolicy)
                .namespace(BASE)
                .build();
        client.start();
    }

    public static void createNode(String path, String data, CreateMode mode) throws Exception {
        if (checkNodeExists(path) == null) {
            String result = client.create().creatingParentsIfNeeded()
                    .withMode(mode).forPath(path, data.getBytes("utf-8"));
            System.out.println(result);
        }
    }

    public static void deleteNode(String path, boolean guaranteed) throws Exception {
        if (checkNodeExists(path) != null) {
            if (guaranteed) {
                client.delete().forPath(path);
//                client.delete().deletingChildrenIfNeeded().forPath(path);
            } else {
                client.delete().guaranteed().deletingChildrenIfNeeded().inBackground().forPath(path);
            }

        }
    }

    private static Stat checkNodeExists(String path) throws Exception {
        Stat stat = client.checkExists().forPath(path);
        return stat;
    }

    public static void main(String[] args) throws Exception {
//        deleteNode(PATH + "/child_5", true);
//        checkNodeExists("/"+BASE +PATH + "/child_5");
//        createNode(PATH + "/child_5", "new child", CreateMode.PERSISTENT);
        deleteNode(PATH + "/child_5", true);
        Thread.sleep(5000);
    }
}
