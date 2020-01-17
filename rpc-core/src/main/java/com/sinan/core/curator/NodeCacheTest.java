package com.sinan.core.curator;

import com.google.common.base.Charsets;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.CountDownLatch;

/**
 * @Author: rocky
 * @Date: Created in 2018/5/14.
 */
public class NodeCacheTest {
    private static final CountDownLatch countDownLatch = new CountDownLatch(1);
    private static final String ADDRESS = "172.17.5.162:2181";
    private static final String PREFIX_SYNC = "/mytest-curator";
    private static final String NAMESPACE = "mytestspace";
    private static CuratorFramework client;
    private static NodeCache nodeCache;
    static {
//        client = CuratorFrameworkFactory.newClient(ADDRESS, 5000, 5000,
//                new ExponentialBackoffRetry(1000, 3));
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 5);
        client = CuratorFrameworkFactory.builder()
                .connectString(ADDRESS)
                .sessionTimeoutMs(10000)
//                .connectionTimeoutMs(5000)
                .retryPolicy(retryPolicy)
                .namespace(NAMESPACE)
                .build();
        client.start();
    }
    private static void initCache() throws Exception {
        client.delete().forPath(PREFIX_SYNC);
        client.create().forPath(PREFIX_SYNC);
        client.setData().forPath(PREFIX_SYNC,"hello curator..".getBytes());
        nodeCache = new NodeCache(client, PREFIX_SYNC);
        nodeCache.start(true);
        startCache(nodeCache);
    }

    private static void startCache(final NodeCache nodeCache) throws Exception {
        ChildData currentData = nodeCache.getCurrentData();
        System.out.println("1111:" + new String(currentData.getData()));
        nodeCache.getListenable().addListener(new NodeCacheListener() {
            public void nodeChanged() throws Exception {
                System.out.println("data change..." + new String(nodeCache.getCurrentData().getData()));
                countDownLatch.countDown();
            }
        });
        Thread.sleep(2000);
        if(client.checkExists().forPath(PREFIX_SYNC) != null){
            System.out.println(new String("设置新内容。。。。".getBytes(), Charsets.UTF_8));
            client.setData().forPath(PREFIX_SYNC, "2222".getBytes());
        }
    }

    public static void main(String[] args) throws Exception {
        initCache();
        countDownLatch.await();
    }
}
