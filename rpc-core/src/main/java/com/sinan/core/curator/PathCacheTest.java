package com.sinan.core.curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

/**
 * @Author: rocky
 * @Date: Created in 2018/5/15.
 */
public class PathCacheTest {
    private static final String PATH = "/mytest-curator/test6";
    private static final String ADDRESS = "172.17.5.162:2181";
    private static final String BASE = "mytestspace";
    private static PathChildrenCache pathChildrenCache;
    private static CuratorFramework client;
    private static CountDownLatch countDownLatch = new CountDownLatch(1);
    private static CountDownLatch countDownLatch2 = new CountDownLatch(5);
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

    public static void main(String[] args) throws Exception {
        startCache();
        countDownLatch.await();
    }

    private static void startCache() throws Exception {
        pathChildrenCache = new PathChildrenCache(client, PATH, true);
        pathChildrenCache.start();
        for (int i = 1; i < 6; i++) {
            String newPath = PATH + "/child_" + i;
            if (checkNodeExists(newPath)==null) {
                String childNodeName = "child_" + i;
                client.create().creatingParentsIfNeeded().forPath(newPath, childNodeName.getBytes());

            }
            countDownLatch2.countDown();
        }
        countDownLatch2.await();
        addListener(pathChildrenCache);
        for(final ChildData childData : pathChildrenCache.getCurrentData()){
            System.out.println("Output: child path :" + childData.getPath() +
                    ", child data: " + new String(childData.getData()));
        }
        Thread.sleep(2000);
        System.out.println("Parent set value......start");//不会有事件监听返回
        client.setData().forPath(PATH, "11111".getBytes());
        System.out.println("Parent set value......end");
        System.out.println("Child del....start");
        client.delete().forPath(PATH + "/child_1");
        System.out.println("Child del....end");
        Thread.sleep(2000);
        for(int j=1; j<3; j++){
            String newPath = PATH + "/child_2/" + j;
            if (checkNodeExists(newPath)==null) {
                String nodeName = "child_2_"+ j;
                client.create().forPath(newPath, nodeName.getBytes());
            }

        }
        addListener(pathChildrenCache);
        System.out.println("second del...start");//不会有事件监听返回
        client.delete().forPath(PATH + "/child_2/2");
        System.out.println("second del...end");
        countDownLatch.countDown();

    }

    private static void addListener(final PathChildrenCache pathChildrenCache) {
        final PathChildrenCacheListener pathChildrenCacheListener = new PathChildrenCacheListener() {
            public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent event) throws Exception {
                System.out.println(
                        "listener child node event: "+event.getType() +
                        ", child node path:" + event.getData().getPath() +
                        ", child node data: " + new String(event.getData().getData()));
            }
        };
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
    }

    private static Stat checkNodeExists(String path) throws Exception {
        Stat stat = client.checkExists().forPath(path);
        return stat;
    }

}
