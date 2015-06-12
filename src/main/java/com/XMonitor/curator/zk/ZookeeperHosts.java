package com.XMonitor.curator.zk;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class ZookeeperHosts {
  private static final Logger log =  LoggerFactory.getLogger(ZookeeperHosts.class);

  private final List<ZookeeperHost> zkHosts;

  public ZookeeperHosts(String addrs) {
    this.zkHosts = Lists.transform(
            Lists.newArrayList(addrs.trim().split(",")), new Function<String, ZookeeperHost>() {
              @Override
              public ZookeeperHost apply(String input) {
                String[] ss = input.trim().split(":", 2);
                return new ZookeeperHost(ss[0], Integer.valueOf(ss[1]));
              }
            }
    );
  }

  public List<JsonValues> hostInfos() {
    return Lists.transform(
            zkHosts, new Function<ZookeeperHost, JsonValues>() {
              @Override
              public JsonValues apply(ZookeeperHost host) {
                return host.getInfo();
              }
            }
    );
  }

  public String sendCommand(String host, String cmd) {
    for (ZookeeperHost zkHost : zkHosts) {
      if (zkHost.toString().equals(host)) {
        return zkHost.command(cmd);
      }
    }
    return String.format("Zookeeper host [%s] not found!", host);
  }

  public void pulse() {
    for (ZookeeperHost host : zkHosts) {
      host.pulse();
    }
  }


//  public static void main(String[] args) throws Exception {
//    Config.init("config");
//    Resources.init();
//
//    final ZookeeperHosts host = new ZookeeperHosts("192.168.10.60:2181,192.168.10.41:2181,192.168.10.42:2181");
//    System.out.println("series: " + Resources.jsonMapper.writeValueAsString(host.hostInfos()));
//
//  }

}
