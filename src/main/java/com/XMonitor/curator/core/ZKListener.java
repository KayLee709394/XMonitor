package com.XMonitor.curator.core;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ksy on 2015/6/11.
 */
public class ZKListener implements IZKListener {


    Logger log = (Logger) LoggerFactory.getLogger(this.getClass());

    private String path;


    public ZKListener(String path) {
        this.path = path;
    }

    @Override
    public void executor(CuratorFramework client) {

        final NodeCache cache = new NodeCache(client, path);
        cache.getListenable().addListener(new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {

                byte[] data = cache.getCurrentData().getData();


                if (data != null) {
                    String dataString = new String(data);

                    System.out.println("Getting node data :" + dataString);
                }
            }
        });
        try {
            cache.start(true);
        } catch (Exception e) {
            log.error("Start NodeCache error for path: {}, error info: {}", path, e.getMessage());
        }
    }
}
