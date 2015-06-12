package com.XMonitor.curator.core;

import org.apache.curator.framework.CuratorFramework;

/**
 * Created by ksy on 2015/6/11.
 */
public interface IZKListener {
    void executor(CuratorFramework client);
}
