package com.XMonitor.curator.core.zk;

import com.XMonitor.curator.core.ws.WebSocketHandler;
import org.apache.curator.RetryPolicy;
import org.apache.curator.ensemble.EnsembleProvider;
import org.apache.curator.ensemble.fixed.FixedEnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.UnhandledErrorListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.nodes.PersistentEphemeralNode;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ksy on 2015/6/11.
 */
@Service("zkFactory")
@Scope("prototype")
public class ZookeeperFactory implements FactoryBean<CuratorFramework>, DisposableBean {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private CuratorFramework zkClient;

    public void setListeners(List<IZKListener> listeners) {
        this.listeners = listeners;
    }

    private List<IZKListener> listeners;


    public void setZkConnectionString(String zkConnectionString) {
        this.zkConnectionString = zkConnectionString;
    }

    private String zkConnectionString;

    @Override
    public CuratorFramework getObject() {
        return zkClient;
    }

    @Override
    public Class<?> getObjectType() {
        return CuratorFramework.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public void destroy() throws Exception {
        zkClient.close();
    }

    public CuratorFramework createClient(String zkConnectionString) throws Exception {

        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        zkClient = createWithOptions(zkConnectionString, retryPolicy, 2000, 10000);
        registerListeners(zkClient);
        zkClient.start();
        PathChildrenCache cache = new PathChildrenCache(zkClient, "/data", true);
//        GetChildrenBuilder childrenBuilder = zkClient.getChildren().
        addListener(cache);
        cache.start();
        return zkClient;
    }

    private String[] getHosts(String zkConnectionString){
        return zkConnectionString.split(",");
    }

    private void addListener(PathChildrenCache cache)
    {
        // a PathChildrenCacheListener is optional. Here, it's used just to log changes
        PathChildrenCacheListener listener = new PathChildrenCacheListener()
        {
            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception
            {
                switch ( event.getType() )
                {
                    case CHILD_ADDED:
                    {
                        logger.info("Node added: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
                        WebSocketHandler.pushState("Node added: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
                        break;
                    }

                    case CHILD_UPDATED:
                    {
                        logger.info("Node changed: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
                        WebSocketHandler.pushState("Node changed: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
                        break;
                    }

                    case CHILD_REMOVED:
                    {
                        logger.info("Node removed: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
                        WebSocketHandler.pushState("Node removed: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
                        break;
                    }
                }
            }
        };
        cache.getListenable().addListener(listener);
    }

    public EnsembleProvider createProvider(String zkConnectionString){

        EnsembleProvider provider = new FixedEnsembleProvider(zkConnectionString);
//        zkClient = createWithOptions(zkConnectionString, retryPolicy, 2000, 10000);
//        registerListeners(zkClient);
        try {
            provider.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return provider;
    }


    public CuratorFramework  createWithOptions(String connectionString, RetryPolicy retryPolicy, int connectionTimeoutMs, int sessionTimeoutMs)
    {
        return CuratorFrameworkFactory.builder()
                .connectString(connectionString)
                .retryPolicy(retryPolicy)
                .connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs)
                .build();
    }


    private void registerListeners(CuratorFramework client){
        client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                logger.info("CuratorFramework state changed: {}", newState);
//                if(newState == ConnectionState.CONNECTED || newState == ConnectionState.RECONNECTED){
//                    for(IZKListener listener : listeners){
//                        listener.executor(client);
//                        logger.info("Listener {} executed!", listener.getClass().getName());
//                    }
//                }
            }
        });

        client.getUnhandledErrorListenable().addListener(new UnhandledErrorListener() {
            @Override
            public void unhandledError(String message, Throwable e) {
                logger.info("CuratorFramework unhandledError: {}", message);
            }
        });
    }
}
